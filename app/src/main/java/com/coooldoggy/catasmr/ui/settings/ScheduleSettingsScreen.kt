package com.coooldoggy.catasmr.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coooldoggy.catasmr.schedule.ScheduleWindow
import com.coooldoggy.catasmr.settings.DetectionSensitivity
import com.coooldoggy.catasmr.settings.PrivacyStatus
import com.coooldoggy.catasmr.settings.VideoQuality
import com.coooldoggy.catasmr.ui.components.TimePickerDialog
import com.coooldoggy.catasmr.ui.components.WindowEditorRow

@Composable
fun ScheduleSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingWindow by remember { mutableStateOf<ScheduleWindow?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Schedule & settings", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Done") }
        }

        if (uiState.windows.isEmpty()) {
            Text("No feeder windows yet — add one to match your auto-feeder's schedule.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(uiState.windows, key = { it.id }) { window ->
                    WindowEditorRow(
                        window = window,
                        onEdit = { viewModel.updateWindow(it) },
                        onDelete = { viewModel.removeWindow(it) },
                        onPickTime = { editingWindow = window }
                    )
                }
            }
        }
        Button(onClick = { viewModel.addWindow() }) { Text("Add window") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        ChoiceRow(
            label = "Detection sensitivity",
            options = DetectionSensitivity.entries,
            selected = uiState.settings.sensitivity,
            optionLabel = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
            onSelect = { viewModel.setSensitivity(it) }
        )
        ChoiceRow(
            label = "Video quality",
            options = VideoQuality.entries,
            selected = uiState.settings.videoQuality,
            optionLabel = { it.name },
            onSelect = { viewModel.setVideoQuality(it) }
        )
        ChoiceRow(
            label = "Upload privacy",
            options = PrivacyStatus.entries,
            selected = uiState.settings.privacyStatus,
            optionLabel = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
            onSelect = { viewModel.setPrivacyStatus(it) }
        )

        ToggleRow("Wi-Fi only uploads", uiState.settings.wifiOnly) { viewModel.setWifiOnly(it) }
        ToggleRow("Keep local copy after upload", uiState.settings.keepLocalCopy) { viewModel.setKeepLocalCopy(it) }
    }

    editingWindow?.let { window ->
        TimePickerDialog(
            initialHour = window.startHour,
            initialMinute = window.startMinute,
            onDismiss = { editingWindow = null },
            onConfirm = { hour, minute ->
                viewModel.updateWindow(window.copy(startHour = hour, startMinute = minute))
                editingWindow = null
            }
        )
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row {
            options.forEach { option ->
                val isSelected = option == selected
                TextButton(onClick = { onSelect(option) }) {
                    Text(
                        optionLabel(option),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
