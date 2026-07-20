package com.coooldoggy.catasmr.schedule

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ScheduleWindow(
    val id: String = UUID.randomUUID().toString(),
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val enabled: Boolean = true,
    val recurrence: RecurrencePattern = RecurrencePattern.Daily,
) {
    init {
        require(startHour in 0..23) { "startHour out of range: $startHour" }
        require(startMinute in 0..59) { "startMinute out of range: $startMinute" }
        require(durationMinutes > 0) { "durationMinutes must be positive: $durationMinutes" }
    }

    /** Minutes since midnight the window starts at. */
    val startOfDayMinutes: Int get() = startHour * 60 + startMinute

    /** Minutes since midnight the window ends at (may exceed 1440 if it crosses midnight). */
    val endOfDayMinutes: Int get() = startOfDayMinutes + durationMinutes

    /** Human-readable recurrence description */
    val recurrenceLabel: String get() = when (recurrence) {
        is RecurrencePattern.Once -> "Once"
        is RecurrencePattern.Daily -> "Daily"
        is RecurrencePattern.Weekly -> {
            when {
                recurrence.daysOfWeek.containsAll((1..7).toList()) -> "Every day"
                recurrence.daysOfWeek == setOf(1, 2, 3, 4, 5) -> "Weekdays"
                recurrence.daysOfWeek == setOf(6, 7) -> "Weekends"
                else -> "Custom weekly"
            }
        }
        is RecurrencePattern.Monthly -> "Monthly (day ${recurrence.dayOfMonth})"
    }
}
