package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.example.R

object ParkingNotificationChannel {

    // v2: channels persist their sound/vibration settings once created on a device, so the
    // custom parking-expiry sound ships in a NEW versioned channel id. Existing installs
    // pick it up without any manual settings change; the retired v1 channel is deleted in
    // createNotificationChannel() so it is never selected for new notifications.
    const val CHANNEL_ID = "curb_system_alerts_v2"
    const val CHANNEL_NAME = "Curb Parking Alerts"
    const val CHANNEL_DESCRIPTION = "Reminders and alerts for active parking sessions"

    // Retired channel from before the custom parking-expiry sound was introduced.
    private const val DEPRECATED_CHANNEL_ID = "curb_system_alerts_v1"

    val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

    // Custom parking-expiry audio shipped in res/raw/parking_session_expired.mp3.
    // Resolved per-app at runtime; no default/system notification tone is used.
    fun soundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.parking_session_expired}")

    val vibrationPattern = longArrayOf(0, 250, 250, 250)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

                // Remove the retired v1 channel so it can no longer be selected.
                manager?.deleteNotificationChannel(DEPRECATED_CHANNEL_ID)

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    vibrationPattern = this@ParkingNotificationChannel.vibrationPattern
                    setSound(soundUri(context), audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                // Idempotent: re-creating an existing channel id is a no-op, so a new channel
                // is NOT created on every notification.
                manager?.createNotificationChannel(channel)
            } catch (_: Throwable) {
                // Ignore channel creation exceptions on custom OEM ROMs
            }
        }
    }
}
