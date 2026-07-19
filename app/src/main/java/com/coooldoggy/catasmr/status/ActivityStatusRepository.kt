package com.coooldoggy.catasmr.status

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.statusDataStore by preferencesDataStore(name = "activity_status")

enum class UploadState { NONE, UPLOADING, SUCCESS, FAILED, NEEDS_REAUTH }

data class ActivityStatus(
    val lastRecordingAtMillis: Long? = null,
    val lastRecordingSuccess: Boolean? = null,
    val lastUploadAtMillis: Long? = null,
    val lastUploadState: UploadState = UploadState.NONE,
    val lastUploadError: String? = null,
)

class ActivityStatusRepository(private val context: Context) {

    private object Keys {
        val LAST_RECORDING_AT = longPreferencesKey("last_recording_at")
        val LAST_RECORDING_SUCCESS = stringPreferencesKey("last_recording_success")
        val LAST_UPLOAD_AT = longPreferencesKey("last_upload_at")
        val LAST_UPLOAD_STATE = stringPreferencesKey("last_upload_state")
        val LAST_UPLOAD_ERROR = stringPreferencesKey("last_upload_error")
    }

    val status: Flow<ActivityStatus> = context.statusDataStore.data.map { prefs ->
        ActivityStatus(
            lastRecordingAtMillis = prefs[Keys.LAST_RECORDING_AT],
            lastRecordingSuccess = prefs[Keys.LAST_RECORDING_SUCCESS]?.toBooleanStrictOrNull(),
            lastUploadAtMillis = prefs[Keys.LAST_UPLOAD_AT],
            lastUploadState = prefs[Keys.LAST_UPLOAD_STATE]?.let {
                runCatching { UploadState.valueOf(it) }.getOrNull()
            } ?: UploadState.NONE,
            lastUploadError = prefs[Keys.LAST_UPLOAD_ERROR],
        )
    }

    suspend fun onRecordingFinished(success: Boolean) {
        context.statusDataStore.edit { prefs ->
            prefs[Keys.LAST_RECORDING_AT] = System.currentTimeMillis()
            prefs[Keys.LAST_RECORDING_SUCCESS] = success.toString()
        }
    }

    suspend fun onUploadStarted() {
        context.statusDataStore.edit { prefs ->
            prefs[Keys.LAST_UPLOAD_STATE] = UploadState.UPLOADING.name
            prefs.remove(Keys.LAST_UPLOAD_ERROR)
        }
    }

    suspend fun onUploadSucceeded() {
        context.statusDataStore.edit { prefs ->
            prefs[Keys.LAST_UPLOAD_AT] = System.currentTimeMillis()
            prefs[Keys.LAST_UPLOAD_STATE] = UploadState.SUCCESS.name
            prefs.remove(Keys.LAST_UPLOAD_ERROR)
        }
    }

    suspend fun onUploadFailed(error: String) {
        context.statusDataStore.edit { prefs ->
            prefs[Keys.LAST_UPLOAD_STATE] = UploadState.FAILED.name
            prefs[Keys.LAST_UPLOAD_ERROR] = error
        }
    }

    suspend fun onNeedsReauth() {
        context.statusDataStore.edit { prefs ->
            prefs[Keys.LAST_UPLOAD_STATE] = UploadState.NEEDS_REAUTH.name
        }
    }
}
