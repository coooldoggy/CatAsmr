package com.coooldoggy.catasmr.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coooldoggy.catasmr.schedule.ScheduleRepository
import com.coooldoggy.catasmr.schedule.ScheduleWindow
import com.coooldoggy.catasmr.schedule.WindowOccurrence
import com.coooldoggy.catasmr.settings.AppSettings
import com.coooldoggy.catasmr.settings.SettingsRepository
import com.coooldoggy.catasmr.status.ActivityStatus
import com.coooldoggy.catasmr.status.ActivityStatusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val status: ActivityStatus = ActivityStatus(),
    val nextWindowLabel: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val scheduleRepository = ScheduleRepository(application)
    private val statusRepository = ActivityStatusRepository(application)

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        statusRepository.status,
        scheduleRepository.windows
    ) { settings, status, windows ->
        HomeUiState(
            settings = settings,
            status = status,
            nextWindowLabel = nextWindowLabel(windows)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun nextWindowLabel(windows: List<ScheduleWindow>): String? {
        val enabled = windows.filter { it.enabled }
        if (enabled.isEmpty()) return null
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val soonestStart = enabled.minOf { WindowOccurrence.containing(it, now).start }
        return soonestStart.format(DateTimeFormatter.ofPattern("EEE h:mm a"))
    }

    fun signOut() {
        viewModelScope.launch { settingsRepository.setAuthorization(false, null) }
    }
}
