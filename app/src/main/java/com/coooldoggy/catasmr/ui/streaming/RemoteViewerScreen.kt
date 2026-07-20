package com.coooldoggy.catasmr.ui.streaming

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

/**
 * Remote viewer screen for watching camera stream from another device
 */
@Composable
fun RemoteViewerScreen(
    deviceName: String,
    streamingState: StateFlow<StreamingState>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by streamingState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Watching: $deviceName",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (state) {
                        is StreamingState.Connected -> "🟢 Connected"
                        is StreamingState.Connecting -> "🟡 Connecting..."
                        is StreamingState.Disconnected -> "⚪ Disconnected"
                        is StreamingState.Error -> "🔴 Error"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        // Video Feed
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is StreamingState.Connected -> {
                    val frame = (state as StreamingState.Connected).currentFrame
                    if (frame != null) {
                        Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = "Camera feed",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }

                is StreamingState.Connecting -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Connecting to $deviceName...",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                is StreamingState.Disconnected -> {
                    Text("Not connected to camera stream")
                }

                is StreamingState.Error -> {
                    Text(
                        "Error: ${(state as StreamingState.Error).message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

sealed class StreamingState {
    data object Connecting : StreamingState()
    data class Connected(val currentFrame: Bitmap?, val fps: Int = 0) : StreamingState()
    data object Disconnected : StreamingState()
    data class Error(val message: String) : StreamingState()
}
