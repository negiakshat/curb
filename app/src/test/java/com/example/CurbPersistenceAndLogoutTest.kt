package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CurbDatabase
import com.example.data.model.DetectedSign
import com.example.data.model.SavedPlace
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.repository.CurbRepository
import com.example.viewmodel.CurbViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CurbPersistenceAndLogoutTest {

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
    fun `fresh local state has empty saved places and empty scan history`() = runBlocking {
        val places = repository.savedPlaces.first()
        val scans = repository.allScans.first()

        assertEquals(0, places.size)
        assertEquals(0, scans.size)
    }

    @Test
    fun `saved places survive logout and are available on login`() = runBlocking {
        val viewModel = CurbViewModel(app)
        viewModel.completeOnboarding(name = "Alex", isGuest = false)

        // 1. User creates saved places
        repository.addSavedPlace(
            SavedPlace(
                name = "Home",
                address = "123 Main St",
                parkingNote = "Park in driveway"
            )
        )
        repository.addSavedPlace(
            SavedPlace(
                name = "Work",
                address = "456 Market St",
                parkingNote = "Metered spot"
            )
        )

        // Verify insertion in repository
        val savedBeforeLogout = repository.savedPlaces.first()
        assertEquals(2, savedBeforeLogout.size)

        // 2. User logs out
        var loggedOut = false
        viewModel.logout { loggedOut = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(loggedOut)
        assertFalse(viewModel.isOnboardingAndPermissionsCompleted())

        // 3. Verify data in Room database is untouched
        val savedAfterLogout = repository.savedPlaces.first()
        assertEquals(2, savedAfterLogout.size)
        assertEquals("Work", savedAfterLogout[0].name)
        assertEquals("Home", savedAfterLogout[1].name)

        // 4. User logs in again (new or existing ViewModel session)
        val returningViewModel = CurbViewModel(app)
        returningViewModel.completeOnboarding(name = "Alex", isGuest = false)
        assertTrue(returningViewModel.isOnboardingAndPermissionsCompleted())

        val savedAfterLogin = repository.savedPlaces.first()
        assertEquals(2, savedAfterLogin.size)
        assertEquals("Work", savedAfterLogin[0].name)
        assertEquals("Home", savedAfterLogin[1].name)
    }

    @Test
    fun `scan history survives logout and is available on login`() = runBlocking {
        val viewModel = CurbViewModel(app)
        viewModel.completeOnboarding(name = "Alex", isGuest = false)

        // 1. Save scan result
        val scan = ScanResult(
            timestamp = System.currentTimeMillis(),
            locationName = "Mission St & 16th",
            cityState = "San Francisco, CA",
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Parking Allowed",
            allowedUntilTime = "6:00 PM",
            timeRemaining = "2h 30m",
            parkingRules = listOf("2 Hour Parking Mon-Fri 8am-6pm"),
            explanation = "You can park here until 6:00 PM.",
            detectedSigns = listOf(
                DetectedSign(
                    id = "sign_1",
                    title = "2 Hour Parking",
                    subtitle = "8:00 AM - 6:00 PM",
                    ruleText = "Mon-Fri"
                )
            ),
            zoneType = "Commercial",
            paymentInfo = "Pay at Meter #402"
        )
        repository.saveScan(scan)

        val scansBeforeLogout = repository.allScans.first()
        assertEquals(1, scansBeforeLogout.size)

        // 2. Logout
        viewModel.logout {}
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // 3. Verify scan history is preserved
        val scansAfterLogout = repository.allScans.first()
        assertEquals(1, scansAfterLogout.size)
        assertEquals("Mission St & 16th", scansAfterLogout[0].locationName)

        // 4. Returning ViewModel session
        val returningViewModel = CurbViewModel(app)
        val scansAfterLogin = repository.allScans.first()
        assertEquals(1, scansAfterLogin.size)
        assertEquals("Mission St & 16th", scansAfterLogin[0].locationName)
    }

    @Test
    fun `explicit deleteAccount wipes local data as requested`() = runBlocking {
        val viewModel = CurbViewModel(app)
        repository.addSavedPlace(
            SavedPlace(
                name = "Gym",
                address = "789 Fitness Blvd",
                parkingNote = ""
            )
        )
        val placesBefore = repository.savedPlaces.first()
        assertEquals(1, placesBefore.size)

        // Delete account
        val deferred = kotlinx.coroutines.CompletableDeferred<Unit>()
        viewModel.deleteAccount { deferred.complete(Unit) }
        var attempts = 0
        while (!deferred.isCompleted && attempts < 20) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(50)
            attempts++
        }

        val placesAfter = repository.savedPlaces.first()
        assertEquals(0, placesAfter.size)
    }
}
