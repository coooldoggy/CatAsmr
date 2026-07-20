package com.coooldoggy.catasmr.ui.streaming

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectToDeviceScreen(
    onBack: () -> Unit,
    onConnect: (deviceName: String, ipAddress: String, port: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deviceName by remember { mutableStateOf("") }
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8888") }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Connect to Camera") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Enter device details to connect to a camera stream",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text("Device Name", style = MaterialTheme.typography.labelMedium)
            TextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                placeholder = { Text("e.g., Living Room Camera") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text("IP Address", style = MaterialTheme.typography.labelMedium)
            TextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                placeholder = { Text("192.168.1.100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text("Port", style = MaterialTheme.typography.labelMedium)
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
                        onConnect(deviceName, ipAddress, portNum)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = deviceName.isNotBlank() && ipAddress.isNotBlank()
            ) {
                Text("Connect")
            }

            Text(
                "To find your device IP:\nadb shell ip addr show | grep 'inet '",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
