package com.coooldoggy.catasmr.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SENSITIVITY = stringPreferencesKey("sensitivity")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val PRIVACY_STATUS = stringPreferencesKey("privacy_status")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEEP_LOCAL_COPY = booleanPreferencesKey("keep_local_copy")
        val IS_AUTHORIZED = booleanPreferencesKey("is_authorized")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            sensitivity = prefs[Keys.SENSITIVITY]?.let { runCatching { DetectionSensitivity.valueOf(it) }.getOrNull() }
                ?: DetectionSensitivity.MEDIUM,
            videoQuality = prefs[Keys.VIDEO_QUALITY]?.let { runCatching { VideoQuality.valueOf(it) }.getOrNull() }
                ?: VideoQuality.HD,
            privacyStatus = prefs[Keys.PRIVACY_STATUS]?.let { runCatching { PrivacyStatus.valueOf(it) }.getOrNull() }
                ?: PrivacyStatus.PRIVATE,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: true,
            keepLocalCopy = prefs[Keys.KEEP_LOCAL_COPY] ?: false,
            isAuthorized = prefs[Keys.IS_AUTHORIZED] ?: false,
            authorizedAccountEmail = prefs[Keys.ACCOUNT_EMAIL],
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
        )
    }

    suspend fun setSensitivity(value: DetectionSensitivity) {
        context.settingsDataStore.edit { it[Keys.SENSITIVITY] = value.name }
    }

    suspend fun setVideoQuality(value: VideoQuality) {
        context.settingsDataStore.edit { it[Keys.VIDEO_QUALITY] = value.name }
    }

    suspend fun setPrivacyStatus(value: PrivacyStatus) {
        context.settingsDataStore.edit { it[Keys.PRIVACY_STATUS] = value.name }
    }

    suspend fun setWifiOnly(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.WIFI_ONLY] = value }
    }

    suspend fun setKeepLocalCopy(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_LOCAL_COPY] = value }
    }

    suspend fun setAuthorization(isAuthorized: Boolean, accountEmail: String?) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IS_AUTHORIZED] = isAuthorized
            if (accountEmail != null) {
                prefs[Keys.ACCOUNT_EMAIL] = accountEmail
            } else {
                prefs.remove(Keys.ACCOUNT_EMAIL)
            }
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }
}
