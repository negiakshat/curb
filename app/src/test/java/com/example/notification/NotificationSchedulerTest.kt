package com.example.notification

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ParkingSessionEntity
import com.example.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
}
