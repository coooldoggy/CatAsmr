package com.coooldoggy.catasmr.streaming

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {
    fun generateQrCode(data: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun generatePairingData(ipAddress: String, port: Int, pairingCode: String): String {
        // Format: ip:port:code
        return "$ipAddress:$port:$pairingCode"
    }

    fun parsePairingData(data: String): Triple<String, Int, String>? {
        return try {
            val parts = data.split(":")
            if (parts.size == 3) {
                val ip = parts[0]
                val port = parts[1].toInt()
                val code = parts[2]
                Triple(ip, port, code)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
