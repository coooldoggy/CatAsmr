package com.coooldoggy.catasmr.schedule

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.ZonedDateTime

@Serializable
sealed class RecurrencePattern {
    /** Occurs only once */
    @Serializable
    data object Once : RecurrencePattern()

    /** Occurs every day */
    @Serializable
    data object Daily : RecurrencePattern()

    /** Occurs on specific days of the week */
    @Serializable
    data class Weekly(val daysOfWeek: Set<Int>) : RecurrencePattern() {
        init {
            require(daysOfWeek.isNotEmpty()) { "At least one day must be selected" }
            require(daysOfWeek.all { it in 1..7 }) { "Day values must be 1-7 (Monday-Sunday)" }
        }
    }

    /** Occurs on specific days of the month */
    @Serializable
    data class Monthly(val dayOfMonth: Int) : RecurrencePattern() {
        init {
            require(dayOfMonth in 1..31) { "dayOfMonth must be 1-31" }
        }
    }

    fun matches(dateTime: ZonedDateTime): Boolean = when (this) {
        is Once -> false
        is Daily -> true
        is Weekly -> {
            val dayOfWeek = dateTime.dayOfWeek.value
            daysOfWeek.contains(dayOfWeek)
        }
        is Monthly -> dateTime.dayOfMonth == dayOfMonth
    }

    companion object {
        fun weekdaysOnly(): Weekly = Weekly(setOf(1, 2, 3, 4, 5))
        fun weekendOnly(): Weekly = Weekly(setOf(6, 7))
        fun twiceDaily(): Weekly = Weekly((1..7).toSet())
    }
}
