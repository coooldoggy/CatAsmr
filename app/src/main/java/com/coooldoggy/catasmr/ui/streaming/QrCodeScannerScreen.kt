package com.coooldoggy.catasmr.ui.streaming

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.coooldoggy.catasmr.R
import com.coooldoggy.catasmr.streaming.QrCodeGenerator
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QrCodeScannerScreen(
    onQrCodeScanned: (ip: String, port: Int, pairingCode: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    var scannedQrCode by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = surfaceProvider
                        }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (scannedQrCode == null) {
                                        try {
                                            val inputImage = InputImage.fromMediaImage(
                                                imageProxy.image!!,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.process(inputImage)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        val qrData = barcode.rawValue
                                                        if (qrData != null && !qrData.isEmpty()) {
                                                            Log.d("QrCodeScanner", "QR code scanned: $qrData")
                                                            scannedQrCode = qrData
                                                            val parsed = QrCodeGenerator.parsePairingData(qrData)
                                                            if (parsed != null) {
                                                                val (ip, port, code) = parsed
                                                                onQrCodeScanned(ip, port, code)
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("QrCodeScanner", "QR code scanning failed", e)
                                                }
                                        } catch (e: Exception) {
                                            Log.e("QrCodeScanner", "Error processing image", e)
                                        } finally {
                                            imageProxy.close()
                                        }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalyzer
                            )
                        } catch (e: Exception) {
                            Log.e("QrCodeScanner", "Error binding camera", e)
                        }
                    }, ctx.mainExecutor)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar with close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
        }

        // Bottom hint
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.qr_scan_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.remote_scan_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}
