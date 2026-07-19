package com.coooldoggy.catasmr.schedule

import java.time.ZonedDateTime

/**
 * Pure, unit-testable math for turning a daily [ScheduleWindow] into a concrete
 * start/end instant relative to "now" — the occurrence currently in progress if
 * [now] falls inside today's window, otherwise the next upcoming one.
 */
object WindowOccurrence {

    data class Occurrence(val start: ZonedDateTime, val end: ZonedDateTime)

    fun containing(window: ScheduleWindow, now: ZonedDateTime): Occurrence {
        var start = now.withHour(window.startHour).withMinute(window.startMinute)
            .withSecond(0).withNano(0)
        var end = start.plusMinutes(window.durationMinutes.toLong())
        if (!now.isBefore(end)) {
            start = start.plusDays(1)
            end = start.plusMinutes(window.durationMinutes.toLong())
        }
        return Occurrence(start, end)
    }
}
