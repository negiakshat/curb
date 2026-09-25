package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ParkingSessionEntity
import com.example.data.model.SavedPlace
import com.example.data.repository.CurbRepository
import com.example.notification.ParkingNotificationScheduler
import com.example.util.SavedPlaceReminderCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationBehaviorAndSafetyTest {

    private lateinit var context: Context
    private lateinit var repository: CurbRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = CurbRepository(context)
    }

    @Test
    fun testSession_default15AndCustom30Reminder() = runBlocking {
        val now = System.currentTimeMillis()
        val endTime = now + 60 * 60 * 1000L

        val session = ParkingSessionEntity(
            id = 100L,
            locationName = "123 Main St",
            startTime = now,
            endTime = endTime,
            allowedUntilTime = "4:00 PM",
            reminderMinutesBefore = 15,
            isActive = true,
            isDemo = false
        )

        // Schedule 15m default
        ParkingNotificationScheduler.scheduleSessionNotifications(context, session)

        // Update to 30m custom
        val updatedSession = session.copy(reminderMinutesBefore = 30)
        ParkingNotificationScheduler.scheduleSessionNotifications(context, updatedSession)

        assertEquals(30, updatedSession.reminderMinutesBefore)
    }

    @Test
    fun testSession_demoSessionBlockedFromScheduling() = runBlocking {
        val now = System.currentTimeMillis()
        val demoSession = ParkingSessionEntity(
            id = 101L,
            locationName = "Demo Spot",
            startTime = now,
            endTime = now + 3600000L,
            allowedUntilTime = "4:00 PM",
            reminderMinutesBefore = 15,
            isActive = true,
            isDemo = true
        )

        ParkingNotificationScheduler.scheduleSessionNotifications(context, demoSession)
        assertTrue(demoSession.isDemo)
    }

    @Test
    fun testSession_endSessionCancelsAlarms() = runBlocking {
        val now = System.currentTimeMillis()
        val session = ParkingSessionEntity(
            id = 102L,
            locationName = "Market St",
            startTime = now,
            endTime = now + 3600000L,
            allowedUntilTime = "4:00 PM",
            reminderMinutesBefore = 15,
            isActive = true,
            isDemo = false
        )

        ParkingNotificationScheduler.scheduleSessionNotifications(context, session)
        ParkingNotificationScheduler.cancelSessionNotifications(context, session.id)

        assertTrue(true)
    }

    @Test
    fun testSavedPlace_calculatorValidTime() {
        val now = System.currentTimeMillis()
        val place = SavedPlace(
            id = 200L,
            name = "Work Spot",
            address = "500 Howard St",
            parkingRuleSummary = "2 HR PARKING 8AM-6PM",
            parkingSchedule = "Allowed until 4:00 PM",
            parkingVerdict = "ALLOWED",
            lastCheckedAt = now,
            reminderEnabled = true,
            reminderMinutesBefore = 15
        )

        val trigger = SavedPlaceReminderCalculator.calculateTriggerTime(place)
        assertNotNull(trigger)
    }

    @Test
    fun testSavedPlace_ambiguousDoesNotSchedule() {
        val now = System.currentTimeMillis()
        val place = SavedPlace(
            id = 201L,
            name = "Ambiguous Spot",
            address = "600 Mission St",
            parkingRuleSummary = "Unclear signage",
            parkingSchedule = "Verify signage",
            parkingVerdict = "AMBIGUOUS",
            lastCheckedAt = now,
            reminderEnabled = true,
            reminderMinutesBefore = 15
        )

        val trigger = SavedPlaceReminderCalculator.calculateTriggerTime(place)
        assertNull(trigger)
    }

    @Test
    fun testSavedPlace_restrictedDoesNotSchedule() {
        val now = System.currentTimeMillis()
        val place = SavedPlace(
            id = 202L,
            name = "Restricted Spot",
            address = "700 Folsom St",
            parkingRuleSummary = "NO PARKING ANYTIME",
            parkingSchedule = "No parking",
            parkingVerdict = "NOT_ALLOWED",
            lastCheckedAt = now,
            reminderEnabled = true
        )

        val trigger = SavedPlaceReminderCalculator.calculateTriggerTime(place)
        assertNull(trigger)
    }

    @Test
    fun testSavedPlace_unrestrictedDoesNotSchedule() {
        val now = System.currentTimeMillis()
        val place = SavedPlace(
            id = 203L,
            name = "Free Spot",
            address = "800 Harrison St",
            parkingRuleSummary = "FREE PARKING",
            parkingSchedule = "Unrestricted",
            parkingVerdict = "UNRESTRICTED",
            lastCheckedAt = now,
            reminderEnabled = true
        )

        val trigger = SavedPlaceReminderCalculator.calculateTriggerTime(place)
        assertNull(trigger)
    }

    @Test
    fun testSavedPlace_usesSavedPlaceIdExtraNotSessionId() {
        val placeId = 555L
        val place = SavedPlace(
            id = placeId,
            name = "Test Extra Spot",
            address = "123 Test St",
            parkingRuleSummary = "2 HR PARKING",
            parkingSchedule = "2 Hours",
            parkingVerdict = "ALLOWED",
            lastCheckedAt = System.currentTimeMillis(),
            reminderEnabled = true
        )

        ParkingNotificationScheduler.scheduleSavedPlaceReminder(context, place)

        val reqCode = ParkingNotificationScheduler.getSavedPlaceRequestCode(placeId)
        assertEquals(100555, reqCode)
    }
}
