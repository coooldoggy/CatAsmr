package com.coooldoggy.catasmr.recording

import com.coooldoggy.catasmr.detection.DetectionConfig

fun interface TimeSource {
    fun nowMs(): Long
}

private val SystemTimeSource = TimeSource { System.currentTimeMillis() }

/**
 * Pure, framework-free WATCHING/RECORDING transition logic driven by cat-detection
 * results and periodic timeout ticks. Unit-testable via an injected [TimeSource].
 */
class RecordingStateMachine(
    private val timeSource: TimeSource = SystemTimeSource,
    private val positiveHitsToStart: Int = DetectionConfig.POSITIVE_HITS_TO_START,
    private val positiveWindowMs: Long = DetectionConfig.POSITIVE_WINDOW_MS,
    private val noDetectionGraceMs: Long = DetectionConfig.NO_DETECTION_GRACE_MS,
    private val maxClipLengthMs: Long = DetectionConfig.MAX_CLIP_LENGTH_MS,
) {

    enum class State { WATCHING, RECORDING }

    sealed interface Action {
        data object StartRecording : Action
        data object StopRecording : Action
        data object None : Action
    }

    var currentState: State = State.WATCHING
        private set

    private val recentPositiveHits = ArrayDeque<Long>()
    private var recordingStartedAtMs: Long? = null
    private var lastPositiveDetectionAtMs: Long? = null

    /** Feed a detection result from the analyzer. */
    fun onDetection(catPresent: Boolean): Action {
        val now = timeSource.nowMs()
        return when (currentState) {
            State.WATCHING -> onDetectionWhileWatching(catPresent, now)
            State.RECORDING -> onDetectionWhileRecording(catPresent, now)
        }
    }

    /** Call periodically (e.g. once/sec) to enforce grace-period and max-length timeouts. */
    fun onTick(): Action {
        if (currentState != State.RECORDING) return Action.None
        val now = timeSource.nowMs()
        return checkRecordingTimeouts(now)
    }

    private fun onDetectionWhileWatching(catPresent: Boolean, now: Long): Action {
        if (!catPresent) {
            recentPositiveHits.clear()
            return Action.None
        }
        recentPositiveHits.addLast(now)
        while (recentPositiveHits.isNotEmpty() && now - recentPositiveHits.first() > positiveWindowMs) {
            recentPositiveHits.removeFirst()
        }
        if (recentPositiveHits.size < positiveHitsToStart) return Action.None

        recentPositiveHits.clear()
        currentState = State.RECORDING
        recordingStartedAtMs = now
        lastPositiveDetectionAtMs = now
        return Action.StartRecording
    }

    private fun onDetectionWhileRecording(catPresent: Boolean, now: Long): Action {
        if (catPresent) lastPositiveDetectionAtMs = now
        return checkRecordingTimeouts(now)
    }

    private fun checkRecordingTimeouts(now: Long): Action {
        val startedAt = recordingStartedAtMs ?: now
        if (now - startedAt >= maxClipLengthMs) return stopRecording()
        val lastPositive = lastPositiveDetectionAtMs ?: startedAt
        if (now - lastPositive >= noDetectionGraceMs) return stopRecording()
        return Action.None
    }

    private fun stopRecording(): Action {
        currentState = State.WATCHING
        recordingStartedAtMs = null
        lastPositiveDetectionAtMs = null
        recentPositiveHits.clear()
        return Action.StopRecording
    }

    fun reset() {
        currentState = State.WATCHING
        recordingStartedAtMs = null
        lastPositiveDetectionAtMs = null
        recentPositiveHits.clear()
    }
}
