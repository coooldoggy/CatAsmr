package com.coooldoggy.catasmr.detection

import com.coooldoggy.catasmr.settings.DetectionSensitivity

object DetectionConfig {

    const val CAT_LABEL = "Cat"
    const val ANALYSIS_INTERVAL_MS = 1000L

    /** Consecutive positive hits (within [POSITIVE_WINDOW_MS]) needed to start recording. */
    const val POSITIVE_HITS_TO_START = 2
    const val POSITIVE_WINDOW_MS = 3_000L

    /** No positive detection for this long while recording -> stop the clip. */
    const val NO_DETECTION_GRACE_MS = 15_000L

    /** Hard safety cap on a single clip's length. */
    const val MAX_CLIP_LENGTH_MS = 5 * 60_000L

    fun confidenceThreshold(sensitivity: DetectionSensitivity): Float = when (sensitivity) {
        DetectionSensitivity.LOW -> 0.5f
        DetectionSensitivity.MEDIUM -> 0.65f
        DetectionSensitivity.HIGH -> 0.8f
    }
}
