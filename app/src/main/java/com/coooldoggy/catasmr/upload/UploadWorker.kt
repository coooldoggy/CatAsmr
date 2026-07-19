package com.coooldoggy.catasmr.upload

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.coooldoggy.catasmr.auth.YouTubeAuthManager
import com.coooldoggy.catasmr.settings.SettingsRepository
import com.coooldoggy.catasmr.status.ActivityStatusRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_VIDEO_URI) ?: return Result.failure()
        val uri = Uri.parse(uriString)

        val statusRepository = ActivityStatusRepository(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        statusRepository.onUploadStarted()

        val authOutcome = YouTubeAuthManager(applicationContext).authorize()
        val accessToken = when (authOutcome) {
            is YouTubeAuthManager.AuthOutcome.Authorized -> authOutcome.accessToken
            is YouTubeAuthManager.AuthOutcome.NeedsConsent -> {
                // Can't show UI from a background worker; surface it on the Home screen
                // and don't retry-loop something a token refresh alone can't fix.
                statusRepository.onNeedsReauth()
                return Result.failure()
            }
            is YouTubeAuthManager.AuthOutcome.Failed -> {
                statusRepository.onUploadFailed(authOutcome.message)
                return Result.retry()
            }
        }

        val settings = settingsRepository.settings.first()
        return try {
            YouTubeUploadClient(applicationContext).upload(
                accessToken = accessToken,
                videoUri = uri,
                title = "Cat eating - ${formattedTimestamp()}",
                description = "Recorded automatically by CatAsmr.",
                privacyStatus = settings.privacyStatus
            )
            statusRepository.onUploadSucceeded()
            if (!settings.keepLocalCopy) {
                applicationContext.contentResolver.delete(uri, null, null)
            }
            Result.success()
        } catch (e: Exception) {
            statusRepository.onUploadFailed(e.message ?: "Upload failed")
            Result.retry()
        }
    }

    private fun formattedTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    companion object {
        const val KEY_VIDEO_URI = "video_uri"
    }
}
