package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ParkingSpot
import com.example.data.repository.CurbRepository
import com.example.viewmodel.CurbViewModel
import kotlinx.coroutines.flow.first
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
class ParkingSpotPersistenceTest {

    private lateinit var app: Application
    private lateinit var repository: CurbRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        repository = CurbRepository(app)
        runBlocking {
            repository.clearAllData()
        }
    }

    @Test
    fun testInitiallyNoActiveParkingSpot() {
        runBlocking {
            val spot = repository.savedParkingSpot.first()
            assertNull(spot)
        }
    }

    @Test
    fun testSavingParkingSpotPersistsData() {
        runBlocking {
            val testLat = 37.774929
            val testLng = -122.419416
            val testAccuracy = 4.5f
            val testTime = 1700000000000L
            val testName = "Valencia St & 16th St"

            val id = repository.saveParkingSpot(
                latitude = testLat,
                longitude = testLng,
                accuracy = testAccuracy,
                timestamp = testTime,
                locationName = testName,
                sessionId = 101L
            )

            assertTrue(id > 0)

            val savedSpot = repository.savedParkingSpot.first()
            assertNotNull(savedSpot)
            savedSpot?.let {
                assertEquals(testLat, it.latitude, 0.0001)
                assertEquals(testLng, it.longitude, 0.0001)
                assertEquals(testAccuracy, it.accuracy ?: 0f, 0.01f)
                assertEquals(testTime, it.timestamp)
                assertEquals(testName, it.locationName)
                assertEquals(101L, it.sessionId)
                assertTrue(it.isActive)
            }
        }
    }

    @Test
    fun testSavedParkingSpotSurvivesLogout() {
        runBlocking {
            val viewModel = CurbViewModel(app)
            viewModel.completeOnboarding(name = "TestUser", isGuest = false)

            repository.saveParkingSpot(
                latitude = 34.052235,
                longitude = -118.243683,
                accuracy = 8.2f,
                timestamp = System.currentTimeMillis(),
                locationName = "Grand Ave",
                sessionId = 1L
            )

            val spotBefore = repository.savedParkingSpot.first()
            assertNotNull(spotBefore)

            // Logout
            var loggedOut = false
            viewModel.logout { loggedOut = true }
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            assertTrue(loggedOut)

            // Spot remains persisted in database after logout
            val spotAfter = repository.savedParkingSpot.first()
            assertNotNull(spotAfter)
            assertEquals(34.052235, spotAfter!!.latitude, 0.0001)
            assertEquals(-118.243683, spotAfter.longitude, 0.0001)
            assertEquals(8.2f, spotAfter.accuracy ?: 0f, 0.01f)
            assertEquals("Grand Ave", spotAfter.locationName)
        }
    }

    @Test
    fun testSavingNewParkingSpotReplacesPreviousActive() {
        runBlocking {
            repository.saveParkingSpot(
                latitude = 37.77,
                longitude = -122.41,
                accuracy = 10f,
                locationName = "First Spot"
            )

            var current = repository.savedParkingSpot.first()
            assertEquals("First Spot", current?.locationName)

            repository.saveParkingSpot(
                latitude = 37.78,
                longitude = -122.40,
                accuracy = 5f,
                locationName = "Second Spot"
            )

            current = repository.savedParkingSpot.first()
            assertNotNull(current)
            assertEquals("Second Spot", current?.locationName)
            assertEquals(37.78, current!!.latitude, 0.0001)
            assertEquals(5f, current.accuracy ?: 0f, 0.01f)
        }
    }
}
