package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

object ParkingNotificationChannel {

    const val CHANNEL_ID = "curb_system_alerts_v1"
    const val CHANNEL_NAME = "Curb Parking Alerts"
    const val CHANNEL_DESCRIPTION = "Reminders and alerts for active parking sessions"

    val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

    val vibrationPattern = longArrayOf(0, 250, 250, 250)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    vibrationPattern = this@ParkingNotificationChannel.vibrationPattern
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                manager?.createNotificationChannel(channel)
            } catch (_: Throwable) {
                // Ignore channel creation exceptions on custom OEM ROMs
            }
        }
    }
}
