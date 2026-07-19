package com.coooldoggy.catasmr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import com.coooldoggy.catasmr.schedule.ScheduleHealthCheckWorker

class CatAsmrApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ScheduleHealthCheckWorker.enqueue(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            RECORDING_CHANNEL_ID,
            "Cat camera activity",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when CatAsmr is watching for or recording your cat"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val RECORDING_CHANNEL_ID = "recording_channel"
    }
}
