package com.coooldoggy.catasmr.recording

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeTimeSource(var now: Long = 0L) : TimeSource {
    override fun nowMs(): Long = now
}

class RecordingStateMachineTest {

    private lateinit var time: FakeTimeSource
    private lateinit var machine: RecordingStateMachine

    @Before
    fun setUp() {
        time = FakeTimeSource(0L)
        machine = RecordingStateMachine(
            timeSource = time,
            positiveHitsToStart = 2,
            positiveWindowMs = 3_000L,
            noDetectionGraceMs = 15_000L,
            maxClipLengthMs = 300_000L
        )
    }

    @Test
    fun `single positive detection does not start recording`() {
        val action = machine.onDetection(true)

        assertEquals(RecordingStateMachine.Action.None, action)
        assertEquals(RecordingStateMachine.State.WATCHING, machine.currentState)
    }

    @Test
    fun `two positive detections within window start recording`() {
        machine.onDetection(true)
        time.now += 1_000L
        val action = machine.onDetection(true)

        assertEquals(RecordingStateMachine.Action.StartRecording, action)
        assertEquals(RecordingStateMachine.State.RECORDING, machine.currentState)
    }

    @Test
    fun `positive hits outside the window do not accumulate`() {
        machine.onDetection(true)
        time.now += 5_000L // outside the 3s window
        val action = machine.onDetection(true)

        assertEquals(RecordingStateMachine.Action.None, action)
        assertEquals(RecordingStateMachine.State.WATCHING, machine.currentState)
    }

    @Test
    fun `negative detection clears accumulated hits while watching`() {
        machine.onDetection(true)
        time.now += 500L
        machine.onDetection(false)
        time.now += 500L
        val action = machine.onDetection(true)

        assertEquals(RecordingStateMachine.Action.None, action)
    }

    @Test
    fun `no detection for grace period stops recording`() {
        startRecording()

        time.now += 15_000L
        val action = machine.onTick()

        assertEquals(RecordingStateMachine.Action.StopRecording, action)
        assertEquals(RecordingStateMachine.State.WATCHING, machine.currentState)
    }

    @Test
    fun `continued detection resets the grace period`() {
        startRecording()

        time.now += 10_000L
        machine.onDetection(true) // resets grace timer
        time.now += 10_000L
        val tickAction = machine.onTick()

        assertEquals(RecordingStateMachine.Action.None, tickAction)
        assertEquals(RecordingStateMachine.State.RECORDING, machine.currentState)
    }

    @Test
    fun `max clip length forces a stop even with continued detection`() {
        startRecording()

        // Keep re-triggering positive detections (well within the grace period) so only
        // the max-clip-length cap -- not the no-detection grace period -- can stop it.
        var stopAction: RecordingStateMachine.Action = RecordingStateMachine.Action.None
        repeat(31) {
            time.now += 10_000L
            val action = machine.onDetection(true)
            if (action == RecordingStateMachine.Action.StopRecording) stopAction = action
        }

        assertEquals(RecordingStateMachine.Action.StopRecording, stopAction)
        assertEquals(RecordingStateMachine.State.WATCHING, machine.currentState)
    }

    private fun startRecording() {
        machine.onDetection(true)
        time.now += 1_000L
        val action = machine.onDetection(true)
        check(action == RecordingStateMachine.Action.StartRecording)
    }
}
