package com.coooldoggy.catasmr.ui.streaming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coooldoggy.catasmr.streaming.DevicePairingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.net.Socket

class RemoteViewerViewModel(context: Context) : ViewModel() {

    private val pairingManager = DevicePairingManager(context)
    private val _streamingState = MutableStateFlow<StreamingState>(StreamingState.Disconnected)
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<String>>(emptyList())
    val pairedDevices: StateFlow<List<String>> = _pairedDevices.asStateFlow()

    private var streamSocket: Socket? = null

    init {
        loadPairedDevices()
    }

    private fun loadPairedDevices() {
        viewModelScope.launch {
            _pairedDevices.value = pairingManager.pairedDevices.value.map { it.deviceName }
        }
    }

    fun connectToDevice(deviceName: String, ipAddress: String, port: Int = 8888) {
        viewModelScope.launch(Dispatchers.IO) {
            var lastError: Exception? = null
            var attempts = 0
            val maxAttempts = 5
            var retryDelay = 500L

            while (attempts < maxAttempts) {
                try {
                    _streamingState.value = StreamingState.Connecting
                    attempts++

                    Log.d(TAG, "Connection attempt $attempts/$maxAttempts to $ipAddress:$port")
                    streamSocket = Socket(ipAddress, port)
                    Log.d(TAG, "Socket connected successfully")

                    val socket = streamSocket ?: return@launch
                    socket.soTimeout = 10000

                    // Send HTTP GET request
                    val request = "GET /stream HTTP/1.1\r\nHost: $ipAddress\r\nConnection: close\r\n\r\n"
                    socket.getOutputStream().write(request.toByteArray())
                    socket.getOutputStream().flush()

                    // Read response and stream frames
                    val reader = socket.getInputStream().bufferedReader()
                    val frameBuffer = ByteArray(1024 * 1024)
                    var line = ""
                    var contentLength = 0

                    // Skip HTTP response headers
                    Log.d(TAG, "Reading HTTP headers...")
                    while (reader.readLine().also { line = it ?: "" }.isNotEmpty()) {
                        Log.d(TAG, "Header: $line")
                    }
                    Log.d(TAG, "Headers done, waiting for frames...")

                    // Read multipart stream
                    val input = BufferedInputStream(socket.getInputStream())
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    var bytesOfCurrentFrame = 0
                    var frameCount = 0
                    val startTime = System.currentTimeMillis()
                    val timeoutMs = 5000L // 5 second timeout for first frame

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check timeout for first frame
                        if (frameCount == 0 && System.currentTimeMillis() - startTime > timeoutMs) {
                            throw Exception("Timeout waiting for first frame (5 seconds)")
                        }
                        var pos = 0

                        while (pos < bytesRead) {
                            if (contentLength == 0 && bytesOfCurrentFrame == 0) {
                                var lineEnd = pos
                                while (lineEnd < bytesRead - 1) {
                                    if (buffer[lineEnd] == '\r'.code.toByte() &&
                                        buffer[lineEnd + 1] == '\n'.code.toByte()) {
                                        val line = String(buffer, pos, lineEnd - pos).trim()
                                        if (line.startsWith("Content-Length:")) {
                                            contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                                        }
                                        pos = lineEnd + 2
                                        break
                                    }
                                    lineEnd++
                                }
                                if (lineEnd >= bytesRead - 1) break

                                if (pos < bytesRead && buffer[pos] == '\r'.code.toByte() &&
                                    pos + 1 < bytesRead && buffer[pos + 1] == '\n'.code.toByte()) {
                                    pos += 2
                                    if (contentLength == 0) continue
                                }
                            }

                            if (contentLength > 0 && bytesOfCurrentFrame < contentLength) {
                                val remaining = bytesRead - pos
                                val toCopy = minOf(remaining, contentLength - bytesOfCurrentFrame)
                                buffer.copyInto(frameBuffer, bytesOfCurrentFrame, pos, pos + toCopy)
                                bytesOfCurrentFrame += toCopy
                                pos += toCopy

                                if (bytesOfCurrentFrame == contentLength) {
                                    try {
                                        val bitmap = BitmapFactory.decodeByteArray(frameBuffer, 0, bytesOfCurrentFrame)
                                        if (bitmap != null) {
                                            frameCount++
                                            _streamingState.value = StreamingState.Connected(bitmap, 10)
                                            Log.d(TAG, "Frame #$frameCount received: $bytesOfCurrentFrame bytes")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error decoding frame ($bytesOfCurrentFrame bytes)", e)
                                    }
                                    contentLength = 0
                                    bytesOfCurrentFrame = 0
                                }
                            } else {
                                pos++
                            }
                        }
                    }

                    disconnect()
                    return@launch // Success, exit retry loop

                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Connection attempt $attempts failed: ${e.message}")

                    // Close socket if needed
                    try {
                        streamSocket?.close()
                    } catch (_: Exception) {}
                    streamSocket = null

                    if (attempts < maxAttempts) {
                        Log.d(TAG, "Retrying in ${retryDelay}ms...")
                        delay(retryDelay)
                        retryDelay = minOf(retryDelay * 2, 5000L) // Exponential backoff, max 5s
                    }
                }
            }

            // All retries exhausted
            Log.e(TAG, "Failed to connect after $maxAttempts attempts", lastError)
            _streamingState.value = StreamingState.Error(
                "Server not ready. ${lastError?.message ?: "Connection refused"}"
            )
        }
    }

    fun disconnect() {
        try {
            streamSocket?.close()
            streamSocket = null
            _streamingState.value = StreamingState.Disconnected
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }

    companion object {
        private const val TAG = "RemoteViewerViewModel"
    }
}
