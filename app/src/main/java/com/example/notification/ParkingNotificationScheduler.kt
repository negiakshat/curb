package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.ParkingSessionEntity

object ParkingNotificationScheduler {

    const val CHANNEL_ID = "curb_parking_channel"
    const val CUSTOM_SOUND_CHANNEL_ID = "curb_parking_alerts_v2"
    const val CHANNEL_NAME = "Parking Alerts"

    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_SAVED_PLACE_ID = "extra_saved_place_id"
    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val EXTRA_TARGET_END_TIME = "extra_target_end_time"
    const val EXTRA_LAST_CHECKED_AT = "extra_last_checked_at"
    const val EXTRA_NAVIGATE_ROUTE = "navigate_route"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

                // Create original channel for backwards compatibility
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders and alerts for active parking sessions"
                    enableVibration(true)
                }
                manager?.createNotificationChannel(channel)

                // Create new channel with custom hatching.mp3 sound
                val resId = context.resources.getIdentifier("hatching", "raw", context.packageName)
                val soundUri = if (resId != 0) {
                    android.net.Uri.parse("android.resource://${context.packageName}/$resId")
                } else {
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                }

                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val customChannel = NotificationChannel(
                    CUSTOM_SOUND_CHANNEL_ID,
                    "Curb Parking Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders and alerts with custom Curb sound"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                manager?.createNotificationChannel(customChannel)
            } catch (_: Throwable) {
                // Ignore channel creation exceptions on custom OEM ROMs
            }
        }
    }

    fun scheduleSessionNotifications(context: Context, session: ParkingSessionEntity) {
        try {
            // DEMO ISOLATION GATE: Demo sessions must NEVER schedule real notification alarms
            if (session.isDemo || session.id <= 0L || !session.isActive) {
                return
            }

            createNotificationChannel(context)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // First cancel any existing alarms for this session to prevent duplicates
            cancelSessionNotifications(context, session.id)

            val now = System.currentTimeMillis()

            // 1. SCHEDULE REMINDER ALARM
            val reminderMinutes = if (session.reminderMinutesBefore > 0) session.reminderMinutesBefore else 15
            val reminderTriggerTime = session.endTime - (reminderMinutes * 60 * 1000L)

            if (reminderTriggerTime > now) {
                val reminderIntent = Intent(context, ParkingNotificationReceiver::class.java).apply {
                    putExtra(EXTRA_SESSION_ID, session.id)
                    putExtra(EXTRA_NOTIFICATION_TYPE, NotificationType.REMINDER.name)
                    putExtra(EXTRA_TARGET_END_TIME, session.endTime)
                }
                val reminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    getReminderRequestCode(session.id),
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, reminderTriggerTime, reminderPendingIntent)
            }

            // 2. SCHEDULE EXPIRATION ALARM
            val expirationTriggerTime = session.endTime
            if (expirationTriggerTime > now) {
                val expirationIntent = Intent(context, ParkingNotificationReceiver::class.java).apply {
                    putExtra(EXTRA_SESSION_ID, session.id)
                    putExtra(EXTRA_NOTIFICATION_TYPE, NotificationType.EXPIRATION.name)
                    putExtra(EXTRA_TARGET_END_TIME, session.endTime)
                }
                val expirationPendingIntent = PendingIntent.getBroadcast(
                    context,
                    getExpirationRequestCode(session.id),
                    expirationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, expirationTriggerTime, expirationPendingIntent)
            }
        } catch (_: Throwable) {
            // Non-fatal notification scheduling error handling
        }
    }

    fun cancelSessionNotifications(context: Context, sessionId: Long) {
        try {
            if (sessionId <= 0L) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // Cancel Reminder
            val reminderIntent = Intent(context, ParkingNotificationReceiver::class.java)
            val reminderPendingIntent = PendingIntent.getBroadcast(
                context,
                getReminderRequestCode(sessionId),
                reminderIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (reminderPendingIntent != null) {
                alarmManager.cancel(reminderPendingIntent)
                reminderPendingIntent.cancel()
            }

            // Cancel Expiration
            val expirationIntent = Intent(context, ParkingNotificationReceiver::class.java)
            val expirationPendingIntent = PendingIntent.getBroadcast(
                context,
                getExpirationRequestCode(sessionId),
                expirationIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (expirationPendingIntent != null) {
                alarmManager.cancel(expirationPendingIntent)
                expirationPendingIntent.cancel()
            }
        } catch (_: Throwable) {}
    }

    fun cancelAll(context: Context) {
        // Can be called when clearing all data
    }

    // Overridable for testing permission states
    var exactAlarmPermissionChecker: (AlarmManager) -> Boolean = { manager ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return if (exactAlarmPermissionChecker(alarmManager)) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    true
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    false
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return true
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return true
            }
        } catch (_: Throwable) {
            // Fallback for devices without exact alarm permission or security exceptions
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Throwable) {}
            return false
        }
    }

    fun scheduleSavedPlaceReminder(context: Context, place: com.example.data.model.SavedPlace) {
        try {
            if (place.id <= 0L || !place.reminderEnabled) {
                cancelSavedPlaceReminder(context, place.id)
                return
            }

            cancelSavedPlaceReminder(context, place.id)

            val triggerTime = com.example.util.SavedPlaceReminderCalculator.calculateTriggerTime(place) ?: return

            createNotificationChannel(context)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, ParkingNotificationReceiver::class.java).apply {
                putExtra(EXTRA_SAVED_PLACE_ID, place.id)
                putExtra(EXTRA_NOTIFICATION_TYPE, NotificationType.SAVED_PLACE.name)
                putExtra(EXTRA_LAST_CHECKED_AT, place.lastCheckedAt)
                putExtra(EXTRA_NAVIGATE_ROUTE, com.example.ui.navigation.Routes.SAVED_PLACES)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getSavedPlaceRequestCode(place.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(alarmManager, triggerTime, pendingIntent)
        } catch (_: Throwable) {}
    }

    fun cancelSavedPlaceReminder(context: Context, placeId: Long) {
        try {
            if (placeId <= 0L) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ParkingNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getSavedPlaceRequestCode(placeId),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (_: Throwable) {}
    }

    fun getSavedPlaceRequestCode(placeId: Long): Int = (100000 + placeId).toInt()

    fun getReminderRequestCode(sessionId: Long): Int = (sessionId * 10 + 1).toInt()
    fun getExpirationRequestCode(sessionId: Long): Int = (sessionId * 10 + 2).toInt()
}
