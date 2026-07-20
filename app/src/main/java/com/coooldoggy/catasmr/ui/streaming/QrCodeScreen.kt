package com.coooldoggy.catasmr.ui.streaming

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QrCodeScreen(
    qrBitmap: Bitmap,
    deviceIp: String,
    pairingCode: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Scan to Connect",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            "다른 기기에서 카메라를 보려면 QR 코드를 스캔하세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // QR Code
        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "Pairing QR Code",
            modifier = Modifier
                .size(300.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
        )

        Text(
            "IP: $deviceIp",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 24.dp),
            fontFamily = FontFamily.Monospace
        )

        Text(
            "Code: $pairingCode",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp
        )
    }
}
