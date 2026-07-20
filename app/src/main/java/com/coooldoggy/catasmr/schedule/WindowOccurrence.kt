package com.coooldoggy.catasmr.schedule

import java.time.ZonedDateTime

/**
 * Pure, unit-testable math for turning a [ScheduleWindow] with recurrence into a concrete
 * start/end instant relative to "now" — the occurrence currently in progress if
 * [now] falls inside the window, otherwise the next upcoming one.
 */
object WindowOccurrence {

    data class Occurrence(val start: ZonedDateTime, val end: ZonedDateTime)

    fun containing(window: ScheduleWindow, now: ZonedDateTime): Occurrence {
        var candidate = now.withHour(window.startHour).withMinute(window.startMinute)
            .withSecond(0).withNano(0)
        var end = candidate.plusMinutes(window.durationMinutes.toLong())

        // Check if now is within today's window
        if (!now.isBefore(end) || !window.recurrence.matches(candidate)) {
            // Find next matching day
            candidate = candidate.plusDays(1)
            var attempts = 0
            while (!window.recurrence.matches(candidate) && attempts < 400) {
                candidate = candidate.plusDays(1)
                attempts++
            }
            end = candidate.plusMinutes(window.durationMinutes.toLong())
        }

        return Occurrence(candidate, end)
    }

    fun nextOccurrence(window: ScheduleWindow, after: ZonedDateTime): Occurrence {
        var candidate = after.withHour(window.startHour).withMinute(window.startMinute)
            .withSecond(0).withNano(0)
            .plusMinutes(1)

        var attempts = 0
        while (!window.recurrence.matches(candidate) && attempts < 400) {
            candidate = candidate.plusDays(1)
            attempts++
        }

        val end = candidate.plusMinutes(window.durationMinutes.toLong())
        return Occurrence(candidate, end)
    }
}
