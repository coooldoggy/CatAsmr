package com.coooldoggy.catasmr.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coooldoggy.catasmr.schedule.AlarmScheduler
import com.coooldoggy.catasmr.schedule.ScheduleRepository
import com.coooldoggy.catasmr.schedule.ScheduleWindow
import com.coooldoggy.catasmr.settings.AppSettings
import com.coooldoggy.catasmr.settings.DetectionSensitivity
import com.coooldoggy.catasmr.settings.PrivacyStatus
import com.coooldoggy.catasmr.settings.SettingsRepository
import com.coooldoggy.catasmr.settings.VideoQuality
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleSettingsUiState(
    val windows: List<ScheduleWindow> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

class ScheduleSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val alarmScheduler = AlarmScheduler(application)

    val uiState: StateFlow<ScheduleSettingsUiState> = combine(
        scheduleRepository.windows,
        settingsRepository.settings
    ) { windows, settings -> ScheduleSettingsUiState(windows, settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleSettingsUiState())

    fun addWindow() {
        viewModelScope.launch {
            val window = ScheduleWindow(startHour = 8, startMinute = 0, durationMinutes = 30)
            scheduleRepository.upsert(window)
            alarmScheduler.schedule(window)
        }
    }

    fun updateWindow(window: ScheduleWindow) {
        viewModelScope.launch {
            scheduleRepository.upsert(window)
            alarmScheduler.schedule(window)
        }
    }

    fun removeWindow(window: ScheduleWindow) {
        viewModelScope.launch {
            scheduleRepository.remove(window.id)
            alarmScheduler.cancel(window.id)
        }
    }

    fun setSensitivity(value: DetectionSensitivity) = viewModelScope.launch { settingsRepository.setSensitivity(value) }

    fun setVideoQuality(value: VideoQuality) = viewModelScope.launch { settingsRepository.setVideoQuality(value) }

    fun setPrivacyStatus(value: PrivacyStatus) = viewModelScope.launch { settingsRepository.setPrivacyStatus(value) }

    fun setWifiOnly(value: Boolean) = viewModelScope.launch { settingsRepository.setWifiOnly(value) }

    fun setKeepLocalCopy(value: Boolean) = viewModelScope.launch { settingsRepository.setKeepLocalCopy(value) }
}
