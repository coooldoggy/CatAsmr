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
}
