package com.coooldoggy.catasmr.ui.streaming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.coooldoggy.catasmr.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectToDeviceScreen(
    onBack: () -> Unit = {},
    onConnect: (deviceName: String, ipAddress: String, port: Int, pairingCode: String?) -> Unit = { _, _, _, _ -> },
    onConnectCloud: (pairingCode: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var deviceName by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8888") }
    var pairingCode by remember { mutableStateOf<String?>(null) }
    var cloudPairingCode by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var isCloudMode by remember { mutableStateOf(false) }

    if (showScanner) {
        QrCodeScannerScreen(
            onQrCodeScanned = { ip, scannedPort, scannedPairingCode ->
                ipAddress = ip
                port = scannedPort.toString()
                pairingCode = scannedPairingCode
                showScanner = false
            },
            onClose = {
                showScanner = false
            },
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(top = 32.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Mode selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isCloudMode = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isCloudMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(if (!isCloudMode) "Local" else "Local", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { isCloudMode = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCloudMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(if (isCloudMode) "Cloud" else "Cloud", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            if (isCloudMode) {
                // Cloud mode
                Text(
                    "Enter the pairing code from the broadcaster device",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Pairing Code", style = MaterialTheme.typography.labelMedium)
                TextField(
                    value = cloudPairingCode,
                    onValueChange = { cloudPairingCode = it.take(6).filter { c -> c.isDigit() } },
                    placeholder = { Text("000000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                )

                Button(
                    onClick = {
                        if (cloudPairingCode.isNotEmpty()) {
                            onConnectCloud(cloudPairingCode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cloudPairingCode.length == 6
                ) {
                    Text("Connect to Cloud")
                }
            } else {
                // Local mode
                Text(
                    "Enter device details to connect to a camera stream",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = { showScanner = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.remote_scan_qr))
                }

                Text(
                    stringResource(R.string.remote_or_manual),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Text(stringResource(R.string.remote_device_name), style = MaterialTheme.typography.labelMedium)
                TextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    placeholder = { Text(stringResource(R.string.remote_device_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(stringResource(R.string.remote_ip_address), style = MaterialTheme.typography.labelMedium)
                TextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    placeholder = { Text(stringResource(R.string.remote_ip_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(stringResource(R.string.remote_port), style = MaterialTheme.typography.labelMedium)
                TextField(
                    value = port,
                    onValueChange = { port = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                )

                Button(
                    onClick = {
                        val portNum = port.toIntOrNull() ?: 8888
                        if (deviceName.isNotBlank() && ipAddress.isNotBlank()) {
                            onConnect(deviceName, ipAddress, portNum, pairingCode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = deviceName.isNotBlank() && ipAddress.isNotBlank()
                ) {
                    Text(stringResource(R.string.remote_connect))
                }

                Text(
                    stringResource(R.string.remote_find_ip),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
