package com.coooldoggy.catasmr.streaming

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CloudStreamingState {
    data object Connecting : CloudStreamingState()
    data class Connected(val frameBitmap: Bitmap?, val fps: Int = 0) : CloudStreamingState()
    data object Disconnected : CloudStreamingState()
    data class Error(val message: String) : CloudStreamingState()
}

class CloudViewerViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val _streamingState = MutableStateFlow<CloudStreamingState>(CloudStreamingState.Disconnected)
    val streamingState: StateFlow<CloudStreamingState> = _streamingState.asStateFlow()

    private var listener: ValueEventListener? = null
    private var frameCount = 0
    private var lastFpsTime = 0L

    fun connectToCloud(pairingCode: String) {
        _streamingState.value = CloudStreamingState.Connecting
        frameCount = 0
        lastFpsTime = System.currentTimeMillis()

        val streamPath = "streams/$pairingCode"
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val data = snapshot.child("data").getValue(String::class.java)
                    if (data != null && data.isNotEmpty()) {
                        val frameBytes = Base64.decode(data, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)

                        frameCount++
                        val now = System.currentTimeMillis()
                        val fps = if (now - lastFpsTime >= 1000) {
                            val currentFps = frameCount
                            frameCount = 0
                            lastFpsTime = now
                            currentFps
                        } else {
                            (frameCount * 1000 / (now - lastFpsTime + 1)).toInt()
                        }

                        _streamingState.value = CloudStreamingState.Connected(bitmap, fps)
                        Log.d(TAG, "Frame received from cloud: $fps fps")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing cloud frame", e)
                    _streamingState.value = CloudStreamingState.Error(e.message ?: "Unknown error")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Cloud stream cancelled: ${error.message}")
                _streamingState.value = CloudStreamingState.Error(error.message)
            }
        }

        database.getReference(streamPath).addValueEventListener(listener!!)
        _streamingState.value = CloudStreamingState.Connected(null, 0)
    }

    fun disconnect() {
        listener?.let {
            val streamPath = "streams/*"
            database.getReference(streamPath).removeEventListener(it)
        }
        _streamingState.value = CloudStreamingState.Disconnected
        Log.d(TAG, "Disconnected from cloud stream")
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }

    companion object {
        private const val TAG = "CloudViewerViewModel"
    }
}
