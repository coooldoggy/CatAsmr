package com.coooldoggy.catasmr.schedule

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.scheduleDataStore by preferencesDataStore(name = "schedule")

class ScheduleRepository(private val context: Context) {

    private object Keys {
        val WINDOWS_JSON = stringPreferencesKey("windows_json")
    }

    val windows: Flow<List<ScheduleWindow>> = context.scheduleDataStore.data.map { prefs ->
        val json = prefs[Keys.WINDOWS_JSON] ?: "[]"
        runCatching { Json.decodeFromString<List<ScheduleWindow>>(json) }.getOrDefault(emptyList())
    }

    suspend fun saveWindows(windows: List<ScheduleWindow>) {
        context.scheduleDataStore.edit { it[Keys.WINDOWS_JSON] = Json.encodeToString(windows) }
    }

    suspend fun upsert(window: ScheduleWindow) {
        val current = windows.first()
        val updated = if (current.any { it.id == window.id }) {
            current.map { if (it.id == window.id) window else it }
        } else {
            current + window
        }
        saveWindows(updated)
    }

    suspend fun remove(id: String) {
        saveWindows(windows.first().filterNot { it.id == id })
    }
}
