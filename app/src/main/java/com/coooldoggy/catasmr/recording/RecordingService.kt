package com.coooldoggy.catasmr.recording

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.coooldoggy.catasmr.detection.BatteryAwareDetector
import com.coooldoggy.catasmr.detection.CatDetector
import com.coooldoggy.catasmr.detection.DetectionConfig
import com.coooldoggy.catasmr.settings.SettingsRepository
import com.coooldoggy.catasmr.status.ActivityStatusRepository
import com.coooldoggy.catasmr.streaming.LocalStreamingServer
import com.coooldoggy.catasmr.streaming.StreamingFrameAnalyzer
import com.coooldoggy.catasmr.upload.UploadQueue
import com.coooldoggy.catasmr.util.PerformanceMonitor
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The only place in the app that touches [androidx.camera.lifecycle.ProcessCameraProvider].
 * Normally started by [com.coooldoggy.catasmr.schedule.WindowAlarmReceiver] at a scheduled
 * window's start/end, but [start]/[stop] can also be called directly (e.g. a manual "start
 * watching now" test button) since the service itself doesn't care who asked it to run.
 * While active it watches for a cat via [CatDetector] and records clips via
 * [CameraController], driven by [RecordingStateMachine].
 */
class RecordingService : LifecycleService() {

    private var recordingStartTime: Long = 0

    // Main dispatcher deliberately: the state machine (mutated from both the ML Kit
    // detection callback and the tick loop below) is not thread-safe, and camera calls
    // require the main thread anyway. ML Kit inference and DataStore I/O run on their
    // own internal executors regardless of this dispatcher.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var cameraController: CameraController
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var statusRepository: ActivityStatusRepository

    private var catDetector: CatDetector? = null
    private var stateMachine: RecordingStateMachine? = null
    private var tickJob: Job? = null
    private var streamingServer: LocalStreamingServer? = null
    private var streamingAnalyzer: StreamingFrameAnalyzer? = null

    override fun onCreate() {
        super.onCreate()
        cameraController = CameraController(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)
        statusRepository = ActivityStatusRepository(applicationContext)
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action != ACTION_START_WINDOW) {
            // Stopping (or any other action) never needs to claim a foreground-service
            // type -- if we're already running, stopForeground()/stopSelf() below is
            // enough; if we're not, we just exit without ever calling startForeground().
            stopWatchingAndSelf()
            return START_NOT_STICKY
        }

        if (!hasCameraPermission() || !hasMicPermission()) {
            // Android 14+ requires actually holding the camera/mic permission *at this
            // moment* to claim those foreground-service types -- declaring them in the
            // manifest isn't enough. And on API 35+, starting a foreground service with
            // no type at all is outright prohibited (crashes the process). So if we're
            // missing a permission, don't call startForeground() at all -- just stop
            // before the "must call startForeground" grace period has any chance to
            // fire. The Home screen's Setup section is what should catch this first.
            stopSelf()
            return START_NOT_STICKY
        }

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else 0
        ServiceCompat.startForeground(
            this,
            RecordingNotification.NOTIFICATION_ID,
            RecordingNotification.build(this, "Watching for your cat…"),
            fgsType
        )

        startWatching()
        return START_NOT_STICKY
    }

    private fun startWatching() {
        serviceScope.launch {
            val settings = settingsRepository.settings.first()
            val machine = RecordingStateMachine()
            stateMachine = machine

            val detector = CatDetector(DetectionConfig.confidenceThreshold(settings.sensitivity)) { catPresent ->
                applyAction(machine.onDetection(catPresent))
            }
            catDetector = detector

            // Set up streaming if enabled
            Log.d(TAG, "Setting up streaming analyzer...")
            val streamingFrameAnalyzer = StreamingFrameAnalyzer { frameData, width, height ->
                Log.d(TAG, "StreamingFrameAnalyzer: Got frame $width x $height (${frameData.size} bytes)")
                streamingServer?.broadcastFrame(frameData, width, height)
            }
            streamingAnalyzer = streamingFrameAnalyzer
            Log.d(TAG, "Streaming analyzer created")

            // Start streaming server
            Log.d(TAG, "Starting LocalStreamingServer on port 8888...")
            streamingServer = LocalStreamingServer(applicationContext, port = 8888)
            streamingServer?.start()
            Log.d(TAG, "Streaming server started")

            cameraController.bind(
                lifecycleOwner = this@RecordingService,
                analyzer = detector,
                videoQuality = settings.videoQuality,
                streamingAnalyzer = streamingFrameAnalyzer,
                onReady = { startTicking(machine) },
                onError = { stopWatchingAndSelf() }
            )
        }
    }

    private fun startTicking(machine: RecordingStateMachine) {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            val analysisInterval = BatteryAwareDetector.getAnalysisInterval(applicationContext)
            Log.d(TAG, "Starting recording with ${analysisInterval}ms analysis interval")
            PerformanceMonitor.logMemoryUsage(applicationContext, "recording_start")

            while (isActive) {
                delay(analysisInterval)
                try {
                    applyAction(machine.onTick())
                } catch (e: Exception) {
                    Log.e(TAG, "Error in recording tick", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    private fun applyAction(action: RecordingStateMachine.Action) {
        when (action) {
            RecordingStateMachine.Action.StartRecording -> {
                recordingStartTime = System.currentTimeMillis()
                cameraController.startRecording { event -> handleRecordEvent(event) }
                updateNotification("Recording your cat…")
                Log.d(TAG, "Started recording")
            }
            RecordingStateMachine.Action.StopRecording -> {
                cameraController.stopRecording()
                updateNotification("Watching for your cat…")
                Log.d(TAG, "Stopped recording")
            }
            RecordingStateMachine.Action.None -> Unit
        }
    }

    private fun handleRecordEvent(event: VideoRecordEvent) {
        if (event !is VideoRecordEvent.Finalize) return
        serviceScope.launch {
            val recordingDuration = System.currentTimeMillis() - recordingStartTime
            if (!event.hasError()) {
                try {
                    val fileSize = applicationContext.contentResolver.query(
                        event.outputResults.outputUri,
                        null, null, null, null
                    )?.use { cursor ->
                        cursor.moveToFirst()
                        val sizeIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.SIZE)
                        cursor.getLong(sizeIndex)
                    } ?: 0L

                    PerformanceMonitor.logRecordingSession(recordingDuration, fileSize)
                    UploadQueue.enqueue(applicationContext, event.outputResults.outputUri)
                    statusRepository.onRecordingFinished(success = true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing recorded video", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    statusRepository.onRecordingFinished(success = false)
                }
            } else {
                Log.w(TAG, "Recording error: ${event.error}")
                statusRepository.onRecordingFinished(success = false)
            }
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(RecordingNotification.NOTIFICATION_ID, RecordingNotification.build(this, text))
    }

    private fun stopWatchingAndSelf() {
        tickJob?.cancel()
        tickJob = null
        cameraController.unbind()
        catDetector?.close()
        catDetector = null
        stateMachine = null
        streamingServer?.stop()
        streamingServer = null
        streamingAnalyzer = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying")
        PerformanceMonitor.logMemoryUsage(applicationContext, "recording_end")
        tickJob?.cancel()
        cameraController.unbind()
        catDetector?.close()
        streamingServer?.stop()
        streamingServer = null
        streamingAnalyzer = null
        serviceScope.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "RecordingService"
        const val ACTION_START_WINDOW = "com.coooldoggy.catasmr.action.SERVICE_START_WINDOW"
        const val ACTION_STOP_WINDOW = "com.coooldoggy.catasmr.action.SERVICE_STOP_WINDOW"
        const val EXTRA_WINDOW_ID = "extra_window_id"

        /** Manual (non-scheduled) start, e.g. a "start watching now" test button. */
        const val MANUAL_WINDOW_ID = "manual"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context, windowId: String = MANUAL_WINDOW_ID) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_WINDOW
                putExtra(EXTRA_WINDOW_ID, windowId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context, windowId: String = MANUAL_WINDOW_ID) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_WINDOW
                putExtra(EXTRA_WINDOW_ID, windowId)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
