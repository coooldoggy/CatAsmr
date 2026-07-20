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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coooldoggy.catasmr.R
import com.coooldoggy.catasmr.recording.RecordingService
import com.coooldoggy.catasmr.status.ActivityStatus
import com.coooldoggy.catasmr.status.UploadState
import com.coooldoggy.catasmr.ui.components.ErrorBanner
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
    var showError by remember { mutableStateOf(false) }

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
    ) {
        uiState.error?.let { error ->
            ErrorBanner(
                error = error,
                onDismiss = {
                    showError = false
                    viewModel.clearError()
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.section_setup), style = MaterialTheme.typography.titleMedium)
        PermissionRow(stringResource(R.string.perm_camera), cameraGranted) {
            permissionLauncher.launch(PermissionUtils.runtimePermissionsToRequest(context).toTypedArray())
        }
        PermissionRow(stringResource(R.string.perm_microphone), micGranted) {
            permissionLauncher.launch(PermissionUtils.runtimePermissionsToRequest(context).toTypedArray())
        }
        PermissionRow(stringResource(R.string.perm_notifications), notifGranted) {
            permissionLauncher.launch(PermissionUtils.runtimePermissionsToRequest(context).toTypedArray())
        }
        PermissionRow(stringResource(R.string.perm_exact_alarms), exactAlarmGranted) {
            context.startActivity(PermissionUtils.exactAlarmSettingsIntent(context))
        }
        PermissionRow(stringResource(R.string.perm_battery_optimization), batteryExempt) {
            PermissionUtils.launchBatteryOptimizationSettings(context)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.section_manual_test), style = MaterialTheme.typography.titleMedium)
        val isWatching by RecordingService.isRunning.collectAsState()
        Text(if (isWatching) stringResource(R.string.recording_watching) else stringResource(R.string.recording_not_watching))
        if (isWatching) {
            Button(onClick = { RecordingService.stop(context) }) { Text(stringResource(R.string.recording_stop_watching)) }
        } else {
            Button(onClick = { RecordingService.start(context) }) { Text(stringResource(R.string.recording_start_watching)) }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.section_schedule), style = MaterialTheme.typography.titleMedium)
        Text(uiState.nextWindowLabel?.let { stringResource(R.string.schedule_next_window, it) } ?: stringResource(R.string.schedule_empty))
        Button(onClick = onOpenSettings) { Text(stringResource(R.string.schedule_edit)) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.section_activity), style = MaterialTheme.typography.titleMedium)
        Text(recordingStatusLabel(context, uiState.status))
        Text(uploadStatusLabel(context, uiState.status))

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.section_youtube), style = MaterialTheme.typography.titleMedium)
        if (uiState.settings.isAuthorized) {
            Text(stringResource(R.string.youtube_signed_in, uiState.settings.authorizedAccountEmail ?: "your account"))
            Button(onClick = { viewModel.signOut() }) { Text(stringResource(R.string.youtube_sign_out)) }
        } else {
            Button(onClick = onSignIn) { Text(stringResource(R.string.youtube_not_signed_in)) }
        }
        }
    }
}

private fun recordingStatusLabel(context: android.content.Context, status: ActivityStatus): String {
    val at = status.lastRecordingAtMillis ?: return context.getString(R.string.recording_no_recordings)
    val timestamp = formatTimestamp(at)
    return if (status.lastRecordingSuccess == true) {
        context.getString(R.string.recording_last_ok, timestamp)
    } else {
        context.getString(R.string.recording_last_failed, timestamp)
    }
}

private fun uploadStatusLabel(context: android.content.Context, status: ActivityStatus): String = when (status.lastUploadState) {
    UploadState.NONE -> context.getString(R.string.upload_none)
    UploadState.UPLOADING -> context.getString(R.string.upload_in_progress)
    UploadState.SUCCESS -> {
        if (status.lastUploadAtMillis != null) {
            context.getString(R.string.upload_success_with_time, formatTimestamp(status.lastUploadAtMillis!!))
        } else {
            context.getString(R.string.upload_success)
        }
    }
    UploadState.FAILED -> context.getString(R.string.upload_failed, status.lastUploadError ?: "unknown error")
    UploadState.NEEDS_REAUTH -> context.getString(R.string.upload_needs_reauth)
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
