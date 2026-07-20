package com.coooldoggy.catasmr.streaming

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Service that streams camera frames to connected remote viewers.
 * Captures frames from the camera and broadcasts them to all connected clients.
 */
class CameraStreamingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectedClients = CopyOnWriteArraySet<StreamingClient>()
    private var frameCapture: FrameCapture? = null

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive.asStateFlow()

    private val _connectedViewers = MutableStateFlow<Int>(0)
    val connectedViewers: StateFlow<Int> = _connectedViewers.asStateFlow()

    inner class StreamingBinder : Binder() {
        fun getService(): CameraStreamingService = this@CameraStreamingService
    }

    override fun onBind(intent: Intent?): IBinder = StreamingBinder()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CameraStreamingService created")
        frameCapture = FrameCapture()
    }

    override fun onDestroy() {
        Log.d(TAG, "CameraStreamingService destroyed")
        stopStreaming()
        frameCapture = null
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Start capturing and broadcasting camera frames
     */
    fun startStreaming() {
        if (_isStreamingActive.value) return
        _isStreamingActive.value = true
        Log.d(TAG, "Started streaming camera frames")
    }

    /**
     * Stop streaming
     */
    fun stopStreaming() {
        if (!_isStreamingActive.value) return
        _isStreamingActive.value = false
        connectedClients.clear()
        _connectedViewers.value = 0
        Log.d(TAG, "Stopped streaming camera frames")
    }

    /**
     * Add a connected client
     */
    fun addClient(client: StreamingClient) {
        connectedClients.add(client)
        _connectedViewers.value = connectedClients.size
        Log.d(TAG, "Client connected: ${client.clientId} (total: ${connectedClients.size})")
    }

    /**
     * Remove a disconnected client
     */
    fun removeClient(clientId: String) {
        connectedClients.removeIf { it.clientId == clientId }
        _connectedViewers.value = connectedClients.size
        Log.d(TAG, "Client disconnected: $clientId (total: ${connectedClients.size})")
    }

    /**
     * Broadcast a frame to all connected clients
     */
    fun broadcastFrame(bitmap: Bitmap) {
        if (connectedClients.isEmpty()) return

        serviceScope.launch {
            try {
                val frameData = compressBitmap(bitmap)
                val framePacket = FramePacket(
                    timestamp = System.currentTimeMillis(),
                    width = bitmap.width,
                    height = bitmap.height,
                    data = frameData
                )

                connectedClients.forEach { client ->
                    try {
                        client.sendFrame(framePacket)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send frame to client ${client.clientId}", e)
                        removeClient(client.clientId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting frame", e)
            }
        }
    }

    private fun compressBitmap(bitmap: Bitmap, quality: Int = 80): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }

    companion object {
        private const val TAG = "CameraStreamingService"

        fun start(context: Context) {
            val intent = Intent(context, CameraStreamingService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CameraStreamingService::class.java)
            context.stopService(intent)
        }
    }
}

/**
 * Represents a connected remote viewer
 */
interface StreamingClient {
    val clientId: String
    suspend fun sendFrame(frame: FramePacket)
}

/**
 * Compressed frame data sent to clients
 */
data class FramePacket(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FramePacket) return false
        if (timestamp != other.timestamp) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Captures frames from ImageAnalysis
 */
class FrameCapture : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        image.close()
    }
}
