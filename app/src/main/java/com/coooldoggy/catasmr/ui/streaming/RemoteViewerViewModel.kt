package com.coooldoggy.catasmr.ui.streaming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coooldoggy.catasmr.streaming.DevicePairingManager
import kotlinx.coroutines.Dispatchers
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
            try {
                _streamingState.value = StreamingState.Connecting
                streamSocket = Socket(ipAddress, port)

                val socket = streamSocket ?: return@launch
                socket.soTimeout = 10000

                // Send HTTP GET request
                val request = "GET /stream HTTP/1.1\r\nHost: $ipAddress\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().write(request.toByteArray())
                socket.getOutputStream().flush()

                // Read response and stream frames
                val input = BufferedInputStream(socket.getInputStream())
                var buffer = ByteArray(8192)
                var bytesRead: Int
                val frameBuffer = mutableListOf<Byte>()
                var inFrame = false

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    for (i in 0 until bytesRead) {
                        frameBuffer.add(buffer[i])

                        // Detect JPEG frame boundaries
                        if (frameBuffer.size >= 2) {
                            val last2 = frameBuffer.takeLast(2)
                            if (last2[0] == 0xFF.toByte() && last2[1] == 0xD8.toByte()) {
                                inFrame = true
                            }
                            if (inFrame && frameBuffer.size >= 4) {
                                val last2b = frameBuffer.takeLast(2)
                                if (last2b[0] == 0xFF.toByte() && last2b[1] == 0xD9.toByte()) {
                                    // Complete frame received
                                    try {
                                        val jpegData = frameBuffer.toByteArray()
                                        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
                                        if (bitmap != null) {
                                            _streamingState.value = StreamingState.Connected(bitmap, 10)
                                        }
                                        frameBuffer.clear()
                                        inFrame = false
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error decoding frame", e)
                                        frameBuffer.clear()
                                    }
                                }
                            }
                        }
                    }
                }

                disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                _streamingState.value = StreamingState.Error(e.message ?: "Unknown error")
                try {
                    streamSocket?.close()
                } catch (_: Exception) {}
                streamSocket = null
            }
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
