package com.coooldoggy.catasmr.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** Arms the start+stop alarms for the current-or-next occurrence of [window]. */
    fun schedule(window: ScheduleWindow, now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())) {
        if (!window.enabled) {
            cancel(window.id)
            return
        }
        val occurrence = WindowOccurrence.containing(window, now)
        setExactAlarm(occurrence.start, startPendingIntent(window.id))
        setExactAlarm(occurrence.end, stopPendingIntent(window.id))
    }

    fun rescheduleAll(windows: List<ScheduleWindow>) {
        windows.forEach { schedule(it) }
    }

    fun cancel(windowId: String) {
        alarmManager.cancel(startPendingIntent(windowId))
        alarmManager.cancel(stopPendingIntent(windowId))
    }

    private fun setExactAlarm(time: ZonedDateTime, pendingIntent: PendingIntent) {
        if (!canScheduleExactAlarms()) return
        val triggerAt = time.toInstant().toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun startPendingIntent(windowId: String): PendingIntent {
        val intent = Intent(context, WindowAlarmReceiver::class.java).apply {
            action = WindowAlarmReceiver.ACTION_START_WINDOW
            putExtra(WindowAlarmReceiver.EXTRA_WINDOW_ID, windowId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(windowId, "start"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopPendingIntent(windowId: String): PendingIntent {
        val intent = Intent(context, WindowAlarmReceiver::class.java).apply {
            action = WindowAlarmReceiver.ACTION_STOP_WINDOW
            putExtra(WindowAlarmReceiver.EXTRA_WINDOW_ID, windowId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(windowId, "stop"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(windowId: String, suffix: String): Int = "$windowId#$suffix".hashCode()
}
