package com.coooldoggy.catasmr.settings

import kotlinx.serialization.Serializable

enum class DetectionSensitivity { LOW, MEDIUM, HIGH }

enum class VideoQuality { SD, HD, FHD }

enum class PrivacyStatus(val apiValue: String) {
    PRIVATE("private"),
    UNLISTED("unlisted")
}

@Serializable
data class AppSettings(
    val sensitivity: DetectionSensitivity = DetectionSensitivity.MEDIUM,
    val videoQuality: VideoQuality = VideoQuality.HD,
    val privacyStatus: PrivacyStatus = PrivacyStatus.PRIVATE,
    val wifiOnly: Boolean = true,
    val keepLocalCopy: Boolean = false,
    val isAuthorized: Boolean = false,
    val authorizedAccountEmail: String? = null,
    val onboardingComplete: Boolean = false,
)
