package com.coooldoggy.catasmr.streaming

import android.util.Base64
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CloudStreamingService(private val pairingCode: String, private val deviceId: String) {
    private val database = FirebaseDatabase.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val streamPath = "streams/$pairingCode"

    fun broadcastFrame(frameData: ByteArray, width: Int, height: Int) {
        scope.launch {
            try {
                val base64Frame = Base64.encodeToString(frameData, Base64.DEFAULT)
                val frameInfo = mapOf(
                    "data" to base64Frame,
                    "width" to width,
                    "height" to height,
                    "timestamp" to System.currentTimeMillis(),
                    "deviceId" to deviceId
                )
                database.getReference(streamPath).setValue(frameInfo)
                Log.d(TAG, "Frame broadcast to cloud: ${frameData.size} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting frame to cloud", e)
            }
        }
    }

    fun stop() {
        // Clean up stream data
        scope.launch {
            try {
                database.getReference(streamPath).removeValue()
                Log.d(TAG, "Cloud stream stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping cloud stream", e)
            }
        }
    }

    companion object {
        private const val TAG = "CloudStreamingService"
    }
}
