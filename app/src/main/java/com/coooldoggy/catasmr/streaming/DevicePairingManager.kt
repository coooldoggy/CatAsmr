package com.coooldoggy.catasmr.streaming

import android.content.Context
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Manages device pairing for remote streaming.
 * Handles pairing codes and device discovery.
 */
class DevicePairingManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("streaming_pairing", Context.MODE_PRIVATE)
    private val database = FirebaseDatabase.getInstance().reference

    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<DevicePair>>(emptyList())
    val pairedDevices: StateFlow<List<DevicePair>> = _pairedDevices.asStateFlow()

    init {
        loadDeviceId()
    }

    private fun loadDeviceId() {
        val id = prefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
        _deviceId.value = id
    }

    /**
     * Generate a pairing code for this device
     */
    fun generatePairingCode(): String {
        // 6-digit code for easy manual entry
        return (100000 + kotlin.random.Random.nextInt(900000)).toString()
    }

    /**
     * Start pairing: create pairing entry in cloud
     */
    suspend fun startPairing(pairingCode: String, deviceName: String) {
        try {
            val deviceId = _deviceId.value ?: return
            val pairEntry = mapOf(
                "code" to pairingCode,
                "deviceId" to deviceId,
                "deviceName" to deviceName,
                "createdAt" to System.currentTimeMillis(),
                "expiresAt" to System.currentTimeMillis() + (5 * 60 * 1000) // 5 min expiry
            )

            database.child("pairing").child(pairingCode).setValue(pairEntry).await()
            Log.d(TAG, "Pairing started with code: $pairingCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start pairing", e)
            throw e
        }
    }

    /**
     * Confirm pairing: validate code and add device
     */
    suspend fun confirmPairing(pairingCode: String, remoteDeviceId: String, remoteDeviceName: String) {
        try {
            val deviceId = _deviceId.value ?: return

            // Validate pairing code exists and is fresh
            val pairingData = database.child("pairing").child(pairingCode).get().await()
            val expiresAt = pairingData.child("expiresAt").getValue(Long::class.java) ?: 0L

            if (System.currentTimeMillis() > expiresAt) {
                throw Exception("Pairing code expired")
            }

            // Add paired device
            val pair = DevicePair(
                deviceId = remoteDeviceId,
                deviceName = remoteDeviceName,
                pairingCode = pairingCode,
                pairedAt = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis()
            )

            // Save locally
            val currentPairs = _pairedDevices.value.toMutableList()
            currentPairs.removeIf { it.deviceId == remoteDeviceId }
            currentPairs.add(pair)
            _pairedDevices.value = currentPairs

            savePairedDevices(currentPairs)

            // Clean up pairing code
            database.child("pairing").child(pairingCode).removeValue().await()
            Log.d(TAG, "Pairing confirmed with device: $remoteDeviceName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to confirm pairing", e)
            throw e
        }
    }

    /**
     * Get device info for streaming (local IP, port, etc)
     */
    suspend fun getDeviceStreamingInfo(deviceId: String): StreamingInfo? {
        return try {
            val info = database.child("devices").child(deviceId).child("streaming").get().await()
            StreamingInfo(
                deviceId = deviceId,
                deviceName = info.child("deviceName").getValue(String::class.java) ?: "Unknown",
                isLocalAvailable = info.child("localAddress").getValue(String::class.java) != null,
                isCloudAvailable = info.child("cloudAvailable").getValue(Boolean::class.java) ?: false,
                localAddress = info.child("localAddress").getValue(String::class.java),
                streamingPort = info.child("port").getValue(Int::class.java) ?: 8888
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device streaming info", e)
            null
        }
    }

    /**
     * Publish this device's streaming info
     */
    suspend fun publishStreamingInfo(
        deviceName: String,
        localAddress: String?,
        port: Int,
        cloudAvailable: Boolean = false
    ) {
        try {
            val deviceId = _deviceId.value ?: return
            val info = mapOf(
                "deviceName" to deviceName,
                "localAddress" to localAddress,
                "port" to port,
                "cloudAvailable" to cloudAvailable,
                "lastUpdated" to System.currentTimeMillis()
            )

            database.child("devices").child(deviceId).child("streaming").setValue(info).await()
            Log.d(TAG, "Published streaming info for device: $deviceName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish streaming info", e)
        }
    }

    private fun savePairedDevices(devices: List<DevicePair>) {
        try {
            val json = devices.joinToString(",") {
                """{"deviceId":"${it.deviceId}","deviceName":"${it.deviceName}","pairedAt":${it.pairedAt}}"""
            }
            prefs.edit().putString("paired_devices", json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save paired devices", e)
        }
    }

    companion object {
        private const val TAG = "DevicePairingManager"
    }
}
