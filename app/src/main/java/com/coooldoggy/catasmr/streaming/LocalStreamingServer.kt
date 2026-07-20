package com.coooldoggy.catasmr.streaming

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Local WiFi streaming server that broadcasts camera frames to connected clients.
 * Uses a simple HTTP-based streaming protocol for compatibility.
 */
class LocalStreamingServer(
    private val context: Context,
    private val port: Int = 8888
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val connectedClients = CopyOnWriteArraySet<ClientConnection>()
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Streaming server started on port $port")
                Log.d(TAG, "Local address: ${getLocalIpAddress()}")

                while (isRunning && !Thread.currentThread().isInterrupted) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        val clientConnection = ClientConnection(clientSocket)
                        connectedClients.add(clientConnection)
                        Log.d(TAG, "Client connected: ${clientConnection.id}")

                        scope.launch {
                            handleClient(clientConnection)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client connection", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Streaming server error", e)
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false

        connectedClients.forEach { it.close() }
        connectedClients.clear()

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }

        serverJob?.cancel()
        Log.d(TAG, "Streaming server stopped")
    }

    fun broadcastFrame(frameData: ByteArray, width: Int, height: Int) {
        if (connectedClients.isEmpty()) {
            Log.d(TAG, "No connected clients, skipping frame broadcast")
            return
        }

        Log.d(TAG, "Broadcasting frame (${frameData.size} bytes) to ${connectedClients.size} clients")
        connectedClients.forEach { client ->
            scope.launch {
                try {
                    client.sendFrame(frameData, width, height)
                    Log.d(TAG, "Frame sent to ${client.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send frame to client ${client.id}: ${e.message}", e)
                    removeClient(client)
                }
            }
        }
    }

    private suspend fun handleClient(client: ClientConnection) {
        try {
            val reader = BufferedReader(InputStreamReader(client.socket.inputStream))
            val requestLine = reader.readLine() ?: return

            Log.d(TAG, "Client request: $requestLine")

            if (requestLine.contains("GET /stream")) {
                client.sendStreamHeader()
                // Keep connection alive for streaming
                while (isRunning && !client.isClosed()) {
                    Thread.sleep(100)
                }
            } else {
                client.sendHttpResponse(404, "Not Found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client ${client.id}", e)
        } finally {
            removeClient(client)
        }
    }

    private fun removeClient(client: ClientConnection) {
        connectedClients.remove(client)
        client.close()
        Log.d(TAG, "Client disconnected: ${client.id}")
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            val ipAddress = connectionInfo?.ipAddress ?: 0
            String.format(
                Locale.US,
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                (ipAddress shr 8) and 0xff,
                (ipAddress shr 16) and 0xff,
                (ipAddress shr 24) and 0xff
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local IP", e)
            "0.0.0.0"
        }
    }

    private inner class ClientConnection(val socket: Socket) {
        val id = "${socket.inetAddress.hostAddress}:${socket.port}"
        private val output = DataOutputStream(socket.outputStream)

        fun sendStreamHeader() {
            val header = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: multipart/x-mixed-replace; boundary=FRAME\r\n")
                append("Connection: keep-alive\r\n")
                append("Cache-Control: no-cache\r\n")
                append("\r\n")
            }
            output.writeBytes(header)
            output.flush()
            Log.d("LocalStreamingServer", "Stream header sent, ready for frames")
        }

        fun sendFrame(frameData: ByteArray, width: Int, height: Int) {
            try {
                val header = buildString {
                    append("--FRAME\r\n")
                    append("Content-Type: image/jpeg\r\n")
                    append("Content-length: ${frameData.size}\r\n")
                    append("X-Timestamp: ${System.currentTimeMillis()}\r\n")
                    append("\r\n")
                }
                output.writeBytes(header)
                output.write(frameData)
                output.writeBytes("\r\n")
                output.flush()
            } catch (e: Exception) {
                throw e
            }
        }

        fun sendHttpResponse(code: Int, message: String) {
            val response = buildString {
                append("HTTP/1.1 $code $message\r\n")
                append("Content-Type: text/plain\r\n")
                append("Content-Length: ${message.length}\r\n")
                append("Connection: close\r\n")
                append("\r\n")
                append(message)
            }
            try {
                output.writeBytes(response)
                output.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending response", e)
            }
        }

        fun isClosed(): Boolean = socket.isClosed

        fun close() {
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket", e)
            }
        }
    }

    companion object {
        private const val TAG = "LocalStreamingServer"
    }
}
