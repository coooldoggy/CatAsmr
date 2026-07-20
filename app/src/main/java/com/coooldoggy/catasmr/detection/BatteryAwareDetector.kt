package com.coooldoggy.catasmr.detection

import android.content.BatteryManager
import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager.BATTERY_HEALTH_GOOD
import android.util.Log

object BatteryAwareDetector {
    private const val TAG = "BatteryAwareDetector"
    private const val LOW_BATTERY_THRESHOLD = 15

    fun getAnalysisInterval(context: Context): Long {
        val batteryLevel = getBatteryLevel(context)
        return when {
            batteryLevel < LOW_BATTERY_THRESHOLD -> {
                Log.i(TAG, "Battery low ($batteryLevel%), reducing detection frequency")
                2000L
            }
            else -> DetectionConfig.ANALYSIS_INTERVAL_MS
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        return try {
            val ifilter = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            (level * 100) / scale
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery level", e)
            100
        }
    }
}
