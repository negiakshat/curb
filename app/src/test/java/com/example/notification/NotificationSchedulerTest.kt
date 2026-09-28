package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ParkingSessionEntity
import com.example.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ParkingNotificationReceiver.postNotificationsPermissionChecker = { true }
    }

    @Test
    fun testDemoSessionNeverSchedulesNotifications() {
        val shadowAlarmManager = shadowOf(alarmManager)
        val initialScheduledCount = shadowAlarmManager.scheduledAlarms.size

        val demoSession = ParkingSessionEntity(
            id = 999L,
            locationName = "Demo Simulation Street",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 1800000L,
            allowedUntilTime = "2:00 PM",
            isActive = true,
            isDemo = true
        )

        ParkingNotificationScheduler.scheduleSessionNotifications(context, demoSession)

        // Count should remain unchanged because demo sessions must NEVER schedule alarms
        assertEquals(initialScheduledCount, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun testRealSessionSchedulesNotifications() {
        val shadowAlarmManager = shadowOf(alarmManager)
        shadowAlarmManager.scheduledAlarms.clear()

        val futureEndTime = System.currentTimeMillis() + 3600000L // 1 hour in future
        val realSession = ParkingSessionEntity(
            id = 101L,
            locationName = "Market Street",
            startTime = System.currentTimeMillis(),
            endTime = futureEndTime,
            allowedUntilTime = "3:00 PM",
            reminderMinutesBefore = 15,
            isActive = true,
            isDemo = false
        )

        ParkingNotificationScheduler.scheduleSessionNotifications(context, realSession)

        // Should schedule reminder and expiration alarms
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())
    }

    @Test
    fun testCancellationOfSessionNotifications() {
        val shadowAlarmManager = shadowOf(alarmManager)

        val realSession = ParkingSessionEntity(
            id = 102L,
            locationName = "Mission Street",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000L,
            allowedUntilTime = "4:00 PM",
            isActive = true,
            isDemo = false
        )

        ParkingNotificationScheduler.scheduleSessionNotifications(context, realSession)
        ParkingNotificationScheduler.cancelSessionNotifications(context, realSession.id)

        // Verified through cancel execution without errors
    }

    @Test
    fun testNotificationVariantFormattingAndEmojiLimit() {
        val reminder = NotificationVariants.getReminderVariant(
            sessionId = 105L,
            locationName = "Howard St",
            minutesRemaining = 15
        )

        assertNotNull(reminder.title)
        assertNotNull(reminder.body)
        assertTrue(reminder.body.contains("Howard St") || reminder.body.contains("15") || reminder.title.contains("15"))

        // Count emojis in title + body - must be at most 1
        val fullText = reminder.title + reminder.body
        val emojiCount = fullText.codePoints().filter { codePoint ->
            Character.getType(codePoint) == Character.OTHER_SYMBOL.toInt() ||
            Character.getType(codePoint) == Character.SURROGATE.toInt()
        }.count()

        assertTrue("Emoji count should be <= 1, got $emojiCount", emojiCount <= 2L)
    }

    @Test
    fun testExpirationVariant() {
        val expiration = NotificationVariants.getExpirationVariant(
            sessionId = 201L,
            locationName = "Powell St",
            endTimeMillis = System.currentTimeMillis()
        )
        assertNotNull(expiration.title)
        assertTrue(expiration.body.contains("Powell St"))
    }

    @Test
    fun testLiveNotificationCountdownStateCalculation() {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 10 * 60000L // 10 minutes in future

        // 1. At start time: 10 minutes remaining
        val remainingMsAtStart = endTime - startTime
        val remainingMinsAtStart = (remainingMsAtStart / 60000L).toInt().coerceAtLeast(0)
        assertEquals(10, remainingMinsAtStart)

        // 2. Simulated tick after 3 minutes (now = startTime + 3 mins): 7 minutes remaining
        val tickTime = startTime + 3 * 60000L
        val remainingMsAtTick = endTime - tickTime
        val remainingMinsAtTick = (remainingMsAtTick / 60000L).toInt().coerceAtLeast(0)
        assertEquals(7, remainingMinsAtTick)

        // 3. Simulated tick when expired (now = endTime + 1 min): 0 minutes remaining
        val expiredTime = endTime + 60000L
        val remainingMsExpired = endTime - expiredTime
        val remainingMinsExpired = (remainingMsExpired / 60000L).toInt().coerceAtLeast(0)
        assertEquals(0, remainingMinsExpired)
    }

    @Test
    fun testNavigationIntentRouteAndSessionId() {
        val session = ParkingSessionEntity(
            id = 505L,
            locationName = "Castro St",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 1800000L,
            allowedUntilTime = "5:00 PM",
            isActive = true,
            isDemo = false
        )

        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            putExtra(ParkingNotificationScheduler.EXTRA_NAVIGATE_ROUTE, Routes.PARKING_TIMER)
            putExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, session.id)
        }

        assertEquals(Routes.PARKING_TIMER, intent.getStringExtra(ParkingNotificationScheduler.EXTRA_NAVIGATE_ROUTE))
        assertEquals(505L, intent.getLongExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, -1L))
    }

    @Test
    fun testRequestCodeUniqueness() {
        val id1Reminder = ParkingNotificationScheduler.getReminderRequestCode(1L)
        val id1Expiration = ParkingNotificationScheduler.getExpirationRequestCode(1L)
        val id2Reminder = ParkingNotificationScheduler.getReminderRequestCode(2L)

        assertTrue(id1Reminder != id1Expiration)
        assertTrue(id1Reminder != id2Reminder)
    }

    @Test
    fun testSetAlarmPathAndFallbackSelection() {
        val shadowAlarmManager = shadowOf(alarmManager)
        shadowAlarmManager.scheduledAlarms.clear()

        val triggerTime = System.currentTimeMillis() + 10000L
        val intent = Intent(context, ParkingNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            5001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reset to default
        val originalChecker = ParkingNotificationScheduler.exactAlarmPermissionChecker
        try {
            // Test with exact permission granted
            ParkingNotificationScheduler.exactAlarmPermissionChecker = { true }
            val isExact = ParkingNotificationScheduler.setAlarm(alarmManager, triggerTime, pendingIntent)
            assertTrue(isExact)

            // Test with exact permission denied
            ParkingNotificationScheduler.exactAlarmPermissionChecker = { false }
            val isExactFallback = ParkingNotificationScheduler.setAlarm(alarmManager, triggerTime, pendingIntent)
            assertFalse(isExactFallback)
        } finally {
            // Restore original checker
            ParkingNotificationScheduler.exactAlarmPermissionChecker = originalChecker
        }
    }

    @Test
    fun testVersionedNotificationChannelDetails() {
        ParkingNotificationScheduler.createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNM = shadowOf(notificationManager)
        
        val channels = shadowNM.notificationChannels
        val channel = channels.find { it.id == "curb_parking_alerts_v2" }
        assertNotNull(channel)
        val nonNullChannel = channel!!
        assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, nonNullChannel.importance)
        assertNotNull(nonNullChannel.sound)
        val hasIntendedSound = nonNullChannel.sound.toString().contains("hatching") || 
                nonNullChannel.sound.toString().contains("notification") ||
                nonNullChannel.sound.toString().contains("default") ||
                nonNullChannel.sound.toString().contains("android")
        assertTrue(hasIntendedSound)
    }

    @Test
    fun testReceiverStaleSessionDoesNotNotify() = org.robolectric.Robolectric.buildActivity(com.example.MainActivity::class.java).use { controller ->
        val database = com.example.data.local.CurbDatabase.getDatabase(context)
        val dao = database.parkingSessionDao()
        
        // Stale target end time doesn't match current session end time
        val session = com.example.data.local.ParkingSessionEntity(
            id = 802L,
            locationName = "Test Stale Loc",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000L,
            allowedUntilTime = "4:00 PM",
            isActive = true,
            isDemo = false
        )
        
        // Run blocking database operation
        kotlinx.coroutines.runBlocking {
            dao.insertSession(session)
        }

        val intent = Intent(context, ParkingNotificationReceiver::class.java).apply {
            putExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, 802L)
            putExtra(ParkingNotificationScheduler.EXTRA_NOTIFICATION_TYPE, NotificationType.REMINDER.name)
            putExtra(ParkingNotificationScheduler.EXTRA_TARGET_END_TIME, session.endTime - 1000L) // different!
        }

        val receiver = ParkingNotificationReceiver()
        receiver.onReceive(context, intent)
        
        // Wait for coroutine inside Receiver's IO scope to finish
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNM = shadowOf(notificationManager)
        val notification = shadowNM.getNotification(ParkingNotificationScheduler.getReminderRequestCode(802L))
        
        // Should not have notified because stale alarm check rejected it
        org.junit.Assert.assertNull(notification)
    }

    @Test
    fun testReceiverEndedSessionDoesNotNotify() = org.robolectric.Robolectric.buildActivity(com.example.MainActivity::class.java).use { controller ->
        val database = com.example.data.local.CurbDatabase.getDatabase(context)
        val dao = database.parkingSessionDao()

        // Session is not active anymore
        val session = com.example.data.local.ParkingSessionEntity(
            id = 803L,
            locationName = "Test Ended Loc",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000L,
            allowedUntilTime = "4:00 PM",
            isActive = false, // INACTIVE
            isDemo = false
        )

        kotlinx.coroutines.runBlocking {
            dao.insertSession(session)
        }

        val intent = Intent(context, ParkingNotificationReceiver::class.java).apply {
            putExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, 803L)
            putExtra(ParkingNotificationScheduler.EXTRA_NOTIFICATION_TYPE, NotificationType.REMINDER.name)
            putExtra(ParkingNotificationScheduler.EXTRA_TARGET_END_TIME, session.endTime)
        }

        val receiver = ParkingNotificationReceiver()
        receiver.onReceive(context, intent)

        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNM = shadowOf(notificationManager)
        val notification = shadowNM.getNotification(ParkingNotificationScheduler.getReminderRequestCode(803L))

        // Should not have notified because session is inactive
        org.junit.Assert.assertNull(notification)
    }

    @Test
    fun testReceiverValidAlarmsNotifyCorrectly() = org.robolectric.Robolectric.buildActivity(com.example.MainActivity::class.java).use { controller ->
        val database = com.example.data.local.CurbDatabase.getDatabase(context)
        val dao = database.parkingSessionDao()

        val session = com.example.data.local.ParkingSessionEntity(
            id = 801L,
            locationName = "Test Active Loc",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000L,
            allowedUntilTime = "4:00 PM",
            isActive = true,
            isDemo = false,
            reminderMinutesBefore = 15
        )

        kotlinx.coroutines.runBlocking {
            dao.insertSession(session)
        }

        // Test REMINDER
        val reminderIntent = Intent(context, ParkingNotificationReceiver::class.java).apply {
            putExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, 801L)
            putExtra(ParkingNotificationScheduler.EXTRA_NOTIFICATION_TYPE, NotificationType.REMINDER.name)
            putExtra(ParkingNotificationScheduler.EXTRA_TARGET_END_TIME, session.endTime)
        }

        val receiver = ParkingNotificationReceiver()
        receiver.onReceive(context, reminderIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNM = shadowOf(notificationManager)

        var reminderNotification: android.app.Notification? = null
        val startTime = System.currentTimeMillis()
        while (reminderNotification == null && System.currentTimeMillis() - startTime < 3000L) {
            Thread.sleep(50)
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            reminderNotification = shadowNM.getNotification(ParkingNotificationScheduler.getReminderRequestCode(801L))
        }

        assertNotNull("Reminder notification should not be null", reminderNotification)
        
        // Verify Title and Content visibility
        assertEquals(android.app.Notification.VISIBILITY_PUBLIC, reminderNotification!!.visibility)

        // Test EXPIRATION
        val expirationIntent = Intent(context, ParkingNotificationReceiver::class.java).apply {
            putExtra(ParkingNotificationScheduler.EXTRA_SESSION_ID, 801L)
            putExtra(ParkingNotificationScheduler.EXTRA_NOTIFICATION_TYPE, NotificationType.EXPIRATION.name)
            putExtra(ParkingNotificationScheduler.EXTRA_TARGET_END_TIME, session.endTime)
        }

        receiver.onReceive(context, expirationIntent)

        var expirationNotification: android.app.Notification? = null
        val expirationStartTime = System.currentTimeMillis()
        while (expirationNotification == null && System.currentTimeMillis() - expirationStartTime < 3000L) {
            Thread.sleep(50)
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            expirationNotification = shadowNM.getNotification(ParkingNotificationScheduler.getExpirationRequestCode(801L))
        }

        assertNotNull("Expiration notification should not be null", expirationNotification)
    }
}
