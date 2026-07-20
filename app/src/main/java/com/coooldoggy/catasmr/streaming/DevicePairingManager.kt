package com.coooldoggy.catasmr.streaming

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DevicePairingManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("streaming_pairing", Context.MODE_PRIVATE)

    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<DevicePair>>(emptyList())
    val pairedDevices: StateFlow<List<DevicePair>> = _pairedDevices.asStateFlow()

    init {
        loadDeviceId()
        loadPairedDevices()
    }

    private fun loadDeviceId() {
        val id = prefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
        _deviceId.value = id
    }

    private fun loadPairedDevices() {
        try {
            val json = prefs.getString("paired_devices", "") ?: ""
            if (json.isNotEmpty()) {
                val devices = mutableListOf<DevicePair>()
                json.split(",").forEach { entry ->
                    if (entry.isNotEmpty()) {
                        val deviceId = entry.substringAfter("\"deviceId\":\"").substringBefore("\"")
                        val deviceName = entry.substringAfter("\"deviceName\":\"").substringBefore("\"")
                        val pairedAt = entry.substringAfter("\"pairedAt\":").substringBefore("}").toLongOrNull() ?: 0L
                        if (deviceId.isNotEmpty()) {
                            devices.add(DevicePair(deviceId, deviceName, "", pairedAt, System.currentTimeMillis()))
                        }
                    }
                }
                _pairedDevices.value = devices
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load paired devices", e)
        }
    }

    fun generatePairingCode(): String {
        return (100000 + kotlin.random.Random.nextInt(900000)).toString()
    }

    fun addPairedDevice(deviceId: String, deviceName: String) {
        val currentPairs = _pairedDevices.value.toMutableList()
        currentPairs.removeIf { it.deviceId == deviceId }
        currentPairs.add(DevicePair(deviceId, deviceName, "", System.currentTimeMillis(), System.currentTimeMillis()))
        _pairedDevices.value = currentPairs
        savePairedDevices(currentPairs)
        Log.d(TAG, "Added paired device: $deviceName")
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
