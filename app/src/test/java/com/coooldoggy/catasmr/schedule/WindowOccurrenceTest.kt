package com.coooldoggy.catasmr.schedule

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class WindowOccurrenceTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun `now before todays start returns todays occurrence`() {
        val window = ScheduleWindow(startHour = 8, startMinute = 0, durationMinutes = 30)
        val now = ZonedDateTime.of(2026, 7, 19, 6, 0, 0, 0, zone)

        val occurrence = WindowOccurrence.containing(window, now)

        assertEquals(ZonedDateTime.of(2026, 7, 19, 8, 0, 0, 0, zone), occurrence.start)
        assertEquals(ZonedDateTime.of(2026, 7, 19, 8, 30, 0, 0, zone), occurrence.end)
    }

    @Test
    fun `now inside todays window returns todays occurrence with a past start`() {
        val window = ScheduleWindow(startHour = 8, startMinute = 0, durationMinutes = 30)
        val now = ZonedDateTime.of(2026, 7, 19, 8, 15, 0, 0, zone)

        val occurrence = WindowOccurrence.containing(window, now)

        assertEquals(ZonedDateTime.of(2026, 7, 19, 8, 0, 0, 0, zone), occurrence.start)
        assertEquals(ZonedDateTime.of(2026, 7, 19, 8, 30, 0, 0, zone), occurrence.end)
    }

    @Test
    fun `now after todays window rolls over to tomorrow`() {
        val window = ScheduleWindow(startHour = 8, startMinute = 0, durationMinutes = 30)
        val now = ZonedDateTime.of(2026, 7, 19, 8, 30, 0, 0, zone)

        val occurrence = WindowOccurrence.containing(window, now)

        assertEquals(ZonedDateTime.of(2026, 7, 20, 8, 0, 0, 0, zone), occurrence.start)
        assertEquals(ZonedDateTime.of(2026, 7, 20, 8, 30, 0, 0, zone), occurrence.end)
    }

    @Test
    fun `window crossing midnight is handled by plain duration addition`() {
        val window = ScheduleWindow(startHour = 23, startMinute = 45, durationMinutes = 30)
        val now = ZonedDateTime.of(2026, 7, 19, 12, 0, 0, 0, zone)

        val occurrence = WindowOccurrence.containing(window, now)

        assertEquals(ZonedDateTime.of(2026, 7, 19, 23, 45, 0, 0, zone), occurrence.start)
        assertEquals(ZonedDateTime.of(2026, 7, 20, 0, 15, 0, 0, zone), occurrence.end)
    }

    @Test
    fun `exactly at end boundary rolls to tomorrow, not treated as still inside`() {
        val window = ScheduleWindow(startHour = 8, startMinute = 0, durationMinutes = 60)
        val now = ZonedDateTime.of(2026, 7, 19, 9, 0, 0, 0, zone)

        val occurrence = WindowOccurrence.containing(window, now)

        assertEquals(ZonedDateTime.of(2026, 7, 20, 8, 0, 0, 0, zone), occurrence.start)
    }
}
