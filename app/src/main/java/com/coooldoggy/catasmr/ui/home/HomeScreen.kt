package com.coooldoggy.catasmr.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coooldoggy.catasmr.recording.RecordingService
import com.coooldoggy.catasmr.status.ActivityStatus
import com.coooldoggy.catasmr.status.UploadState
import com.coooldoggy.catasmr.ui.components.PermissionRow
import com.coooldoggy.catasmr.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    val cameraGranted = remember(refreshTick) { PermissionUtils.hasCameraPermission(context) }
    val micGranted = remember(refreshTick) { PermissionUtils.hasMicPermission(context) }
    val notifGranted = remember(refreshTick) { PermissionUtils.hasNotificationPermission(context) }
    val exactAlarmGranted = remember(refreshTick) { PermissionUtils.canScheduleExactAlarms(context) }
    val batteryExempt = remember(refreshTick) { PermissionUtils.isIgnoringBatteryOptimizations(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("CatAsmr", style = MaterialTheme.typography.headlineMedium)

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Setup", style = MaterialTheme.typography.titleMedium)
        PermissionRow("Camera", cameraGranted) {
            permissionLauncher.launch(PermissionUtils.runtimePermissionsToRequest(context).toTypedArray())
        }
        PermissionRow("Microphone", micGranted) {
            permissionLauncher.launch(PermissionUtils.runtimePermissionsToRequest(context).toTypedArray())
        }
        PermissionRow("Notifications", notifGranted) {
            permissionLauncher.launch(PermissionUtils.runtimePermissionsToRequest(context).toTypedArray())
        }
        PermissionRow("Exact alarms", exactAlarmGranted) {
            context.startActivity(PermissionUtils.exactAlarmSettingsIntent(context))
        }
        PermissionRow("Battery optimization disabled", batteryExempt) {
            PermissionUtils.launchBatteryOptimizationSettings(context)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Manual test", style = MaterialTheme.typography.titleMedium)
        val isWatching by RecordingService.isRunning.collectAsState()
        Text(if (isWatching) "Currently watching for your cat" else "Not watching — bypasses the schedule")
        if (isWatching) {
            Button(onClick = { RecordingService.stop(context) }) { Text("Stop watching") }
        } else {
            Button(onClick = { RecordingService.start(context) }) { Text("Start watching now") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Schedule", style = MaterialTheme.typography.titleMedium)
        Text(uiState.nextWindowLabel?.let { "Next window: $it" } ?: "No feeder schedule set yet")
        Button(onClick = onOpenSettings) { Text("Edit schedule & settings") }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Activity", style = MaterialTheme.typography.titleMedium)
        Text(recordingStatusLabel(uiState.status))
        Text(uploadStatusLabel(uiState.status))

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("YouTube", style = MaterialTheme.typography.titleMedium)
        if (uiState.settings.isAuthorized) {
            Text("Signed in as ${uiState.settings.authorizedAccountEmail ?: "your account"}")
            Button(onClick = { viewModel.signOut() }) { Text("Sign out") }
        } else {
            Button(onClick = onSignIn) { Text("Sign in to YouTube") }
        }
    }
}

private fun recordingStatusLabel(status: ActivityStatus): String {
    val at = status.lastRecordingAtMillis ?: return "No recordings yet"
    val outcome = if (status.lastRecordingSuccess == true) "ok" else "failed"
    return "Last recording: ${formatTimestamp(at)} ($outcome)"
}

private fun uploadStatusLabel(status: ActivityStatus): String = when (status.lastUploadState) {
    UploadState.NONE -> "No uploads yet"
    UploadState.UPLOADING -> "Uploading…"
    UploadState.SUCCESS -> "Last upload succeeded" +
        (status.lastUploadAtMillis?.let { " (${formatTimestamp(it)})" } ?: "")
    UploadState.FAILED -> "Last upload failed: ${status.lastUploadError ?: "unknown error"}"
    UploadState.NEEDS_REAUTH -> "YouTube access needs to be reconnected — tap Sign in"
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
