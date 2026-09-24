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
    const val CHANNEL_NAME = "Parking Alerts"

    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val EXTRA_TARGET_END_TIME = "extra_target_end_time"
    const val EXTRA_NAVIGATE_ROUTE = "navigate_route"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders and alerts for active parking sessions"
                    enableVibration(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
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

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: Throwable) {
            // Fallback for devices without exact alarm permission or security exceptions
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Throwable) {}
        }
    }

    fun scheduleSavedPlaceReminder(context: Context, place: com.example.data.model.SavedPlace) {
        try {
            if (place.id <= 0L || !place.reminderEnabled) return
            createNotificationChannel(context)
            cancelSavedPlaceReminder(context, place.id)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = System.currentTimeMillis()

            // Calculate trigger time if schedule has an explicit allowed time limit or target
            // Default: 15 minutes before limit, or trigger if schedule is active
            val reminderMins = if (place.reminderMinutesBefore > 0) place.reminderMinutesBefore else 15
            val triggerTime = place.lastCheckedAt + (60 * 60 * 1000L) - (reminderMins * 60 * 1000L)

            if (triggerTime > now) {
                val intent = Intent(context, ParkingNotificationReceiver::class.java).apply {
                    putExtra(EXTRA_SESSION_ID, place.id)
                    putExtra(EXTRA_NOTIFICATION_TYPE, NotificationType.REMINDER.name)
                    putExtra(EXTRA_NAVIGATE_ROUTE, "saved_places")
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    getSavedPlaceRequestCode(place.id),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setAlarm(alarmManager, triggerTime, pendingIntent)
            }
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
