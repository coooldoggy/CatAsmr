package com.coooldoggy.catasmr.streaming

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Captures camera frames and sends them to the streaming server.
 * Converts YUV frames to JPEG for efficient network transmission.
 */
class StreamingFrameAnalyzer(
    private val onFrameReady: (ByteArray, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastFrameTime = 0L
    private val frameIntervalMs = 100 // ~10 FPS for streaming

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastFrameTime < frameIntervalMs) {
            image.close()
            return
        }
        lastFrameTime = now

        try {
            Log.d(TAG, "Processing frame format=${image.format} ${image.width}x${image.height}")
            val bitmap = imageProxyToBitmap(image)
            if (bitmap != null) {
                val jpegData = compressBitmapToJpeg(bitmap, quality = 60)
                Log.d(TAG, "Encoded to JPEG: ${jpegData.size} bytes")
                onFrameReady(jpegData, bitmap.width, bitmap.height)
                bitmap.recycle()
            } else {
                Log.w(TAG, "Failed to convert image to bitmap")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing streaming frame", e)
        } finally {
            image.close()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return when (image.format) {
            ImageFormat.YUV_420_888 -> yuvToRgb(image)
            ImageFormat.NV21 -> nv21ToRgb(image)
            else -> {
                Log.w(TAG, "Unsupported image format: ${image.format}")
                null
            }
        }
    }

    private fun yuvToRgb(image: ImageProxy): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        buffer.rewind()

        val data = ByteArray(buffer.capacity() +
            (planes[1].buffer.capacity() + planes[2].buffer.capacity()))
        buffer.get(data, 0, planes[0].buffer.capacity())
        planes[1].buffer.get(data, planes[0].buffer.capacity(), planes[1].buffer.capacity())
        planes[2].buffer.get(data, planes[0].buffer.capacity() + planes[1].buffer.capacity(),
            planes[2].buffer.capacity())

        val yuvImage = YuvImage(data, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 80, out)
        val jpegBytes = out.toByteArray()

        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    private fun nv21ToRgb(image: ImageProxy): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        buffer.rewind()
        val data = ByteArray(buffer.capacity())
        buffer.get(data)

        val yuvImage = YuvImage(data, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 80, out)
        val jpegBytes = out.toByteArray()

        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    private fun compressBitmapToJpeg(bitmap: Bitmap, quality: Int = 60): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }

    companion object {
        private const val TAG = "StreamingFrameAnalyzer"
    }
}
