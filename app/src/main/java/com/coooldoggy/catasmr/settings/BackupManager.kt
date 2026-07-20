package com.coooldoggy.catasmr.settings

import android.content.Context
import android.util.Log
import com.coooldoggy.catasmr.schedule.ScheduleWindow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val windows: List<ScheduleWindow> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

object BackupManager {
    private const val TAG = "BackupManager"
    private const val BACKUP_FILE = "catasmr_backup.json"

    suspend fun createBackup(
        context: Context,
        windows: List<ScheduleWindow>,
        settings: AppSettings
    ): Result<String> = try {
        val backup = BackupData(
            version = 1,
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            windows = windows,
            settings = settings
        )
        val json = Json.encodeToString(BackupData.serializer(), backup)
        val file = File(context.cacheDir, BACKUP_FILE)
        file.writeText(json)
        Log.i(TAG, "Backup created: ${file.absolutePath}")
        Result.success(json)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create backup", e)
        Result.failure(e)
    }

    suspend fun restoreBackup(
        context: Context,
        json: String
    ): Result<BackupData> = try {
        val backup = Json.decodeFromString(BackupData.serializer(), json)
        require(backup.version == 1) { "Unsupported backup version: ${backup.version}" }
        Log.i(TAG, "Backup restored: ${backup.windows.size} windows, exported at ${backup.exportedAt}")
        Result.success(backup)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to restore backup", e)
        Result.failure(e)
    }

    fun getLastBackupFile(context: Context): File? {
        val file = File(context.cacheDir, BACKUP_FILE)
        return if (file.exists()) file else null
    }
}
