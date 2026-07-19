package com.coooldoggy.catasmr.upload

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.coooldoggy.catasmr.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object UploadQueue {

    suspend fun enqueue(context: Context, videoUri: Uri) {
        val wifiOnly = SettingsRepository(context).settings.first().wifiOnly
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(Data.Builder().putString(UploadWorker.KEY_VIDEO_URI, videoUri.toString()).build())
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            videoUri.toString(),
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
