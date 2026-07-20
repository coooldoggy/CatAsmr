package com.coooldoggy.catasmr.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"

    fun logMemoryUsage(context: Context, tag: String = "app") {
        try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val totalMemory = runtime.totalMemory() / 1024 / 1024
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            val usedMemory = totalMemory - freeMemory

            val nativeHeap = Debug.getNativeHeap().sumOf { it.sizeMb }.toLong()

            val info = "Memory - Used: ${usedMemory}MB / ${maxMemory}MB, Native: ${nativeHeap}MB"
            Log.i(TAG, tag + ": " + info)

            FirebaseCrashlytics.getInstance().log("$tag memory: used=${usedMemory}MB max=${maxMemory}MB native=${nativeHeap}MB")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log memory usage", e)
        }
    }

    fun logAppStartup(context: Context, startTimeMs: Long) {
        val duration = System.currentTimeMillis() - startTimeMs
        Log.i(TAG, "App startup time: ${duration}ms")
        FirebaseCrashlytics.getInstance().log("app_startup_ms=$duration")
    }

    fun logRecordingSession(duration: Long, fileSize: Long) {
        val durationSeconds = duration / 1000
        val fileSizeMb = fileSize / 1024 / 1024
        Log.i(TAG, "Recording session: ${durationSeconds}s, ${fileSizeMb}MB")
        FirebaseCrashlytics.getInstance().log("recording_duration_s=$durationSeconds, file_size_mb=$fileSizeMb")
    }

    fun logUploadEvent(success: Boolean, fileSize: Long, durationMs: Long) {
        val fileSizeMb = fileSize / 1024 / 1024
        val status = if (success) "success" else "failed"
        Log.i(TAG, "Upload: $status, ${fileSizeMb}MB, ${durationMs}ms")
        FirebaseCrashlytics.getInstance().log("upload_$status: size_mb=$fileSizeMb duration_ms=$durationMs")
    }
}
