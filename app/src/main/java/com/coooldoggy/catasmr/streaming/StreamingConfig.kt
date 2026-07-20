package com.coooldoggy.catasmr.streaming

import kotlinx.serialization.Serializable

@Serializable
data class StreamingConfig(
    val enabled: Boolean = false,
    val localStreamingEnabled: Boolean = true,
    val cloudStreamingEnabled: Boolean = false,
    val streamingPort: Int = 8888,
    val pairedDevices: Set<String> = emptySet(),
)

@Serializable
data class DevicePair(
    val deviceId: String,
    val deviceName: String,
    val pairingCode: String,
    val pairedAt: Long,
    val lastSeen: Long,
)

@Serializable
data class StreamingInfo(
    val deviceId: String,
    val deviceName: String,
    val isLocalAvailable: Boolean,
    val isCloudAvailable: Boolean,
    val localAddress: String?,
    val streamingPort: Int,
)
