package com.coooldoggy.catasmr.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coooldoggy.catasmr.R
import com.coooldoggy.catasmr.recording.RecordingService
import com.coooldoggy.catasmr.status.ActivityStatus
import com.coooldoggy.catasmr.status.UploadState
import com.coooldoggy.catasmr.streaming.QrCodeGenerator
import com.coooldoggy.catasmr.ui.components.ErrorBanner
import com.coooldoggy.catasmr.ui.components.PermissionRow
import com.coooldoggy.catasmr.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    onOpenRemoteViewer: () -> Unit = {},
    onOpenPreview: () -> Unit = {},
    onSignIn: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var expandPermissions by remember { mutableStateOf(false) }

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

    val allPermissionsGranted = cameraGranted && micGranted && notifGranted && exactAlarmGranted && batteryExempt

    Column(modifier = modifier.fillMaxSize().padding(top = 32.dp)) {
        uiState.error?.let { error ->
            ErrorBanner(
                error = error,
                onDismiss = { viewModel.clearError() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isWatching by RecordingService.isRunning.collectAsState()
            val streamingInfo by RecordingService.streamingInfo.collectAsState()

            // Recording Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWatching) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.home_recording_status), style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (isWatching) stringResource(R.string.home_active) else stringResource(R.string.home_inactive),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isWatching) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isWatching) {
                        Button(
                            onClick = { RecordingService.stop(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(stringResource(R.string.home_stop_watching), color = MaterialTheme.colorScheme.onTertiary)
                        }
                        Button(
                            onClick = onOpenPreview,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.home_view_preview))
                        }

                        // QR Code Card
                        streamingInfo?.let { info ->
                            info.localAddress?.let { ip ->
                                info.pairingCode?.let { code ->
                                    val qrData = QrCodeGenerator.generatePairingData(ip, info.streamingPort, code)
                                    val qrBitmap = remember(qrData) { QrCodeGenerator.generateQrCode(qrData, 256) }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                stringResource(R.string.home_share_qr),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Image(
                                                bitmap = qrBitmap.asImageBitmap(),
                                                contentDescription = "Pairing QR Code",
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .fillMaxWidth(0.6f)
                                            )
                                            Text(
                                                code,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                RecordingService.start(context)
                                viewModel.delayedOpenPreview(onOpenPreview)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(stringResource(R.string.home_start_watching), color = MaterialTheme.colorScheme.onTertiary)
                        }
                    }
                }
            }

            // Remote Viewing Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.home_remote_viewer), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.home_remote_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenRemoteViewer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.home_connect_camera))
                    }
                }
            }

            // Schedule Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.home_feeding_schedule), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        uiState.nextWindowLabel?.let { stringResource(R.string.schedule_next_window, it) } ?: stringResource(R.string.home_no_schedule),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.home_edit_schedule))
                    }
                }
            }

            // Activity Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.home_activity), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        recordingStatusLabel(context, uiState.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        uploadStatusLabel(context, uiState.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Permissions Card - only show if not all granted
            if (!allPermissionsGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { expandPermissions = !expandPermissions },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.home_permissions_required),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        if (expandPermissions) {
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
                        }
                    }
                }
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
