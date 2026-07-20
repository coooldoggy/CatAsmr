package com.coooldoggy.catasmr.schedule

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Low-frequency safety net: re-arms every window's alarms so a device that silently
 * cleared them (e.g. an OEM "battery cleaner") without a reboot still self-heals.
 * Not a second scheduling system — [AlarmScheduler.schedule] is idempotent to call again.
 */
class ScheduleHealthCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val windows = ScheduleRepository(applicationContext).windows.first()
        AlarmScheduler(applicationContext).rescheduleAll(windows)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "schedule_health_check"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduleHealthCheckWorker>(6, TimeUnit.HOURS)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
