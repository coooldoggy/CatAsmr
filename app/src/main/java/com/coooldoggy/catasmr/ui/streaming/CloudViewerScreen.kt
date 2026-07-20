package com.coooldoggy.catasmr.ui.streaming

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coooldoggy.catasmr.streaming.CloudStreamingState
import com.coooldoggy.catasmr.streaming.CloudViewerViewModel

@Composable
fun CloudViewerScreen(
    pairingCode: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudViewerViewModel = viewModel()
) {
    val streamingState by viewModel.streamingState.collectAsState()

    DisposableEffect(pairingCode) {
        viewModel.connectToCloud(pairingCode)
        onDispose {
            viewModel.disconnect()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        when (val state = streamingState) {
            CloudStreamingState.Connecting -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Connecting to cloud stream...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            is CloudStreamingState.Connected -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.frameBitmap != null) {
                            Image(
                                bitmap = state.frameBitmap.asImageBitmap(),
                                contentDescription = "Cloud Stream",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Waiting for frames...")
                        }

                        // FPS and close button
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    shape = MaterialTheme.shapes.small
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        // FPS indicator at top-left
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(
                                "${state.fps} fps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    shape = MaterialTheme.shapes.small
                                ).padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            CloudStreamingState.Disconnected -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Disconnected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tap to close and try again",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            is CloudStreamingState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Connection Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
