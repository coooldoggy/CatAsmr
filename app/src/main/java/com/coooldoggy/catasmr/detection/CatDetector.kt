package com.coooldoggy.catasmr.detection

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/**
 * CameraX [ImageAnalysis.Analyzer] that runs ML Kit's bundled (offline) image labeler
 * against a throttled stream of frames and reports whether a cat is currently visible.
 * Analysis frames are independent of the higher-resolution recording stream.
 */
class CatDetector(
    confidenceThreshold: Float,
    private val onResult: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(confidenceThreshold).build()
    )

    private var lastAnalyzedAtMs = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedAtMs < DetectionConfig.ANALYSIS_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        lastAnalyzedAtMs = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        labeler.process(inputImage)
            .addOnSuccessListener { labels ->
                val catDetected = labels.any { it.text.equals(DetectionConfig.CAT_LABEL, ignoreCase = true) }
                onResult(catDetected)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun close() {
        labeler.close()
    }
}
