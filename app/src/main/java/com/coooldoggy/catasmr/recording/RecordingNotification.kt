package com.coooldoggy.catasmr.recording

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.coooldoggy.catasmr.CatAsmrApp
import com.coooldoggy.catasmr.MainActivity
import com.coooldoggy.catasmr.R

object RecordingNotification {
    const val NOTIFICATION_ID = 1001

    fun build(context: Context, contentText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CatAsmrApp.RECORDING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("CatAsmr")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
