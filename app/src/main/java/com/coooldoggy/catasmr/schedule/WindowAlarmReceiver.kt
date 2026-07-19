package com.coooldoggy.catasmr.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coooldoggy.catasmr.recording.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WindowAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val windowId = intent.getStringExtra(EXTRA_WINDOW_ID) ?: return
        val appContext = context.applicationContext

        when (intent.action) {
            ACTION_START_WINDOW -> RecordingService.start(appContext, windowId)
            ACTION_STOP_WINDOW -> {
                RecordingService.stop(appContext, windowId)

                // Re-arm tomorrow's occurrence now that today's has ended.
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val window = ScheduleRepository(appContext).windows.first().find { it.id == windowId }
                        if (window != null) {
                            AlarmScheduler(appContext).schedule(window)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_START_WINDOW = "com.coooldoggy.catasmr.action.START_WINDOW"
        const val ACTION_STOP_WINDOW = "com.coooldoggy.catasmr.action.STOP_WINDOW"
        const val EXTRA_WINDOW_ID = "extra_window_id"
    }
}
