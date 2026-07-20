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
import com.coooldoggy.catasmr.ui.util.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleSettingsUiState(
    val windows: List<ScheduleWindow> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val error: UiError? = null,
)

class ScheduleSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepository = ScheduleRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val alarmScheduler = AlarmScheduler(application)

    private val _error = MutableStateFlow<UiError?>(null)

    val uiState: StateFlow<ScheduleSettingsUiState> = combine(
        scheduleRepository.windows,
        settingsRepository.settings,
        _error
    ) { windows, settings, error ->
        ScheduleSettingsUiState(windows, settings, isLoading = false, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleSettingsUiState())

    fun addWindow() {
        viewModelScope.launch {
            try {
                val window = ScheduleWindow(startHour = 8, startMinute = 0, durationMinutes = 30)
                scheduleRepository.upsert(window)
                alarmScheduler.schedule(window)
                _error.value = null
            } catch (e: Exception) {
                _error.value = UiError.OperationFailed("Failed to add window")
            }
        }
    }

    fun updateWindow(window: ScheduleWindow) {
        viewModelScope.launch {
            try {
                scheduleRepository.upsert(window)
                alarmScheduler.schedule(window)
                _error.value = null
            } catch (e: Exception) {
                _error.value = UiError.OperationFailed("Failed to update window")
            }
        }
    }

    fun removeWindow(window: ScheduleWindow) {
        viewModelScope.launch {
            try {
                scheduleRepository.remove(window.id)
                alarmScheduler.cancel(window.id)
                _error.value = null
            } catch (e: Exception) {
                _error.value = UiError.OperationFailed("Failed to remove window")
            }
        }
    }

    fun setSensitivity(value: DetectionSensitivity) = viewModelScope.launch {
        try {
            settingsRepository.setSensitivity(value)
            _error.value = null
        } catch (e: Exception) {
            _error.value = UiError.OperationFailed("Failed to update sensitivity")
        }
    }

    fun setVideoQuality(value: VideoQuality) = viewModelScope.launch {
        try {
            settingsRepository.setVideoQuality(value)
            _error.value = null
        } catch (e: Exception) {
            _error.value = UiError.OperationFailed("Failed to update video quality")
        }
    }

    fun setPrivacyStatus(value: PrivacyStatus) = viewModelScope.launch {
        try {
            settingsRepository.setPrivacyStatus(value)
            _error.value = null
        } catch (e: Exception) {
            _error.value = UiError.OperationFailed("Failed to update privacy status")
        }
    }

    fun setWifiOnly(value: Boolean) = viewModelScope.launch {
        try {
            settingsRepository.setWifiOnly(value)
            _error.value = null
        } catch (e: Exception) {
            _error.value = UiError.OperationFailed("Failed to update wifi setting")
        }
    }

    fun setKeepLocalCopy(value: Boolean) = viewModelScope.launch {
        try {
            settingsRepository.setKeepLocalCopy(value)
            _error.value = null
        } catch (e: Exception) {
            _error.value = UiError.OperationFailed("Failed to update local copy setting")
        }
    }

    fun clearError() {
        _error.value = null
    }
}
