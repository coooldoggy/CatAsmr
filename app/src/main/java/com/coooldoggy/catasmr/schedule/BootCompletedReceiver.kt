package com.coooldoggy.catasmr.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val windows = ScheduleRepository(appContext).windows.first()
                AlarmScheduler(appContext).rescheduleAll(windows)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
