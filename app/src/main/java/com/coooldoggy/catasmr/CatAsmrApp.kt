package com.coooldoggy.catasmr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import com.coooldoggy.catasmr.schedule.ScheduleHealthCheckWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CatAsmrApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        initCrashlytics()
        createNotificationChannel()
        ScheduleHealthCheckWorker.enqueue(this)
    }

    private fun initCrashlytics() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            crashlytics.recordException(exception)
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", exception)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            RECORDING_CHANNEL_ID,
            getString(R.string.notification_channel_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val RECORDING_CHANNEL_ID = "recording_channel"
        private const val TAG = "CatAsmrApp"
    }
}
