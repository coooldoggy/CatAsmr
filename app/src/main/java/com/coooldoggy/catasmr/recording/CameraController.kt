package com.coooldoggy.catasmr.recording

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.coooldoggy.catasmr.settings.VideoQuality
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Owns the single [ProcessCameraProvider] binding for the app: a low-res [ImageAnalysis]
 * stream (for cat detection) and a full-res [VideoCapture] stream (for recording), bound
 * together with no [androidx.camera.core.Preview] use case since nothing displays them.
 */
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val analysisExecutor: Executor = Executors.newSingleThreadExecutor()

    @SuppressLint("MissingPermission") // CAMERA permission verified by the caller before bind()
    fun bind(
        lifecycleOwner: LifecycleOwner,
        analyzer: ImageAnalysis.Analyzer,
        videoQuality: VideoQuality,
        streamingAnalyzer: ImageAnalysis.Analyzer? = null,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val quality = toQuality(videoQuality)
                val qualitySelector = QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(quality))
                val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
                val capture = VideoCapture.withOutput(recorder)
                videoCapture = capture

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor, analyzer)

                provider.unbindAll()

                val useCases = mutableListOf(capture, analysis)
                if (streamingAnalyzer != null) {
                    val streamingAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(android.util.Size(640, 480)) // Lower res for streaming
                        .build()
                    streamingAnalysis.setAnalyzer(analysisExecutor, streamingAnalyzer)
                    useCases.add(streamingAnalysis)
                }

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases.toTypedArray()
                )
                onReady()
            } catch (t: Throwable) {
                onError(t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission") // RECORD_AUDIO permission verified by the caller
    fun startRecording(onEvent: (VideoRecordEvent) -> Unit): Boolean {
        val capture = videoCapture ?: return false
        if (activeRecording != null) return false

        val name = "CatAsmr_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CatAsmr")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val pendingRecording = capture.output.prepareRecording(context, outputOptions)
            .withAudioEnabled()

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                activeRecording = null
            }
            onEvent(event)
        }
        return true
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun isRecording(): Boolean = activeRecording != null

    fun unbind() {
        stopRecording()
        cameraProvider?.unbindAll()
        cameraProvider = null
        videoCapture = null
    }

    private fun toQuality(videoQuality: VideoQuality): Quality = when (videoQuality) {
        VideoQuality.SD -> Quality.SD
        VideoQuality.HD -> Quality.HD
        VideoQuality.FHD -> Quality.FHD
    }
}
