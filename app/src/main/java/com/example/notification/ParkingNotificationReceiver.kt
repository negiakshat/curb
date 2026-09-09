package com.example.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.CurbDatabase
import com.example.data.local.SessionPreferences
import com.example.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ParkingNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, -1L)
        val typeStr = intent.getStringExtra(ParkingNotificationScheduler.EXTRA_NOTIFICATION_TYPE) ?: return
        val targetEndTime = intent.getLongExtra(ParkingNotificationScheduler.EXTRA_TARGET_END_TIME, 0L)

        if (sessionId <= 0L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                processNotification(context, sessionId, typeStr, targetEndTime)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processNotification(
        context: Context,
        sessionId: Long,
        typeStr: String,
        targetEndTime: Long
    ) {
        // 1. CHECK USER PUSH NOTIFICATION PREFERENCE
        val sessionPrefs = SessionPreferences(context)
        val userProfile = sessionPrefs.getUserProfile()
        if (!userProfile.pushNotificationsEnabled) {
            return
        }

        // 2. CHECK POST_NOTIFICATIONS PERMISSION ON API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // 3. QUERY DATABASE FOR AUTHORITATIVE SESSION STATE
        val database = CurbDatabase.getDatabase(context)
        val session = database.parkingSessionDao().getSessionById(sessionId) ?: return

        // 4. REAL STATE & DEMO ISOLATION VALIDATION
        if (session.isDemo) {
            // DEMO ISOLATION GATE: Demo sessions must NEVER trigger real notifications
            return
        }

        if (!session.isActive) {
            // SESSION STATE GATE: Ended or cancelled sessions must not notify
            return
        }

        if (targetEndTime > 0 && session.endTime != targetEndTime) {
            // STALE ALARM GATE: Session was extended or updated
            return
        }

        // 5. SELECT NOTIFICATION VARIANT
        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (_: Exception) {
            NotificationType.REMINDER
        }

        val notificationText = when (type) {
            NotificationType.REMINDER -> {
                val remainingMinutes = session.reminderMinutesBefore.coerceAtLeast(1)
                NotificationVariants.getReminderVariant(
                    sessionId = session.id,
                    locationName = session.locationName,
                    minutesRemaining = remainingMinutes
                )
            }
            NotificationType.EXPIRATION -> {
                NotificationVariants.getExpirationVariant(
                    sessionId = session.id,
                    locationName = session.locationName,
                    endTimeMillis = session.endTime
                )
            }
            NotificationType.ENDED -> {
                NotificationVariants.getEndedVariant(
                    sessionId = session.id,
                    locationName = session.locationName
                )
            }
        }

        // 6. BUILD CONTENT INTENT FOR NAVIGATION & CONTEXT RESTORATION
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ParkingNotificationScheduler.EXTRA_NAVIGATE_ROUTE, Routes.PARKING_TIMER)
            putExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, session.id)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            sessionId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 7. BUILD AND POST NATIVE NOTIFICATION
        ParkingNotificationScheduler.createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, ParkingNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationText.title)
            .setContentText(notificationText.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = if (type == NotificationType.REMINDER) {
            ParkingNotificationScheduler.getReminderRequestCode(sessionId)
        } else {
            ParkingNotificationScheduler.getExpirationRequestCode(sessionId)
        }

        notificationManager?.notify(notificationId, notification)
    }
}
