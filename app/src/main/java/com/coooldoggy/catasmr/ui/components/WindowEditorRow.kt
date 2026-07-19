package com.coooldoggy.catasmr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coooldoggy.catasmr.schedule.ScheduleWindow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DURATION_PRESETS_MIN = listOf(15, 30, 45, 60, 90)
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun WindowEditorRow(
    window: ScheduleWindow,
    onEdit: (ScheduleWindow) -> Unit,
    onDelete: (ScheduleWindow) -> Unit,
    onPickTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    var durationMenuExpanded by remember { mutableStateOf(false) }
    val timeLabel = remember(window.startHour, window.startMinute) {
        LocalTime.of(window.startHour, window.startMinute).format(TIME_FORMATTER)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPickTime) { Text(timeLabel) }
                Switch(checked = window.enabled, onCheckedChange = { onEdit(window.copy(enabled = it)) })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    TextButton(onClick = { durationMenuExpanded = true }) {
                        Text("${window.durationMinutes} min")
                    }
                    DropdownMenu(
                        expanded = durationMenuExpanded,
                        onDismissRequest = { durationMenuExpanded = false }
                    ) {
                        DURATION_PRESETS_MIN.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text("$minutes min") },
                                onClick = {
                                    onEdit(window.copy(durationMinutes = minutes))
                                    durationMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                TextButton(onClick = { onDelete(window) }) { Text("Remove") }
            }
        }
    }
}
