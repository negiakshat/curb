package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.repository.CurbRepository
import com.example.viewmodel.CurbViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParkingTimerResultTest {

    private lateinit var application: Application
    private lateinit var repository: CurbRepository
    private lateinit var viewModel: CurbViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        viewModel = CurbViewModel(application)
        // Extract the same repository instance the ViewModel is using
        val repoField = CurbViewModel::class.java.getDeclaredField("repository")
        repoField.isAccessible = true
        repository = repoField.get(viewModel) as CurbRepository
        
        runBlocking {
            val db = com.example.data.local.CurbDatabase.getDatabase(application)
            db.scanDao().clearAllScans()
            db.parkingSessionDao().clearAllSessions()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `repository startParkingSession success with valid signs`() = runTest {
        val allowedScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Verified",
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "Valid scan",
            detectedSigns = listOf(
                com.example.data.model.DetectedSign(
                    id = "sign_1",
                    title = "2 Hour Parking",
                    rawText = "2 Hour Parking 8 AM - 6 PM",
                    croppedImageUri = "file:///fake/sign.jpg"
                )
            )
        )
        val scanId = repository.saveScan(allowedScan)
        println("Saved scan ID: $scanId")
        assertTrue(scanId > 0)

        val sessionId = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "Direct Test Spot",
            durationMinutes = 60
        )
        println("Started session ID: $sessionId")

        assertTrue("Expected positive session ID, got $sessionId", sessionId > 0)
        val active = repository.activeSession.first()
        assertNotNull(active)
        assertEquals(sessionId, active?.id)
    }

    @Test
    fun `viewModel startParkingSession propagates success result`() = runTest {
        println("Starting viewModel success test")
        val allowedScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Verified",
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "Valid scan",
            isDemo = true
        )
        val scanId = repository.saveScan(allowedScan)
        println("Saved scan for VM test: $scanId")
        
        var capturedId = -2L
        viewModel.startParkingSession(
            scanResultId = scanId,
            scanResult = allowedScan,
            locationName = "ViewModel Test Spot",
            durationMinutes = 60,
            onResult = { id -> 
                println("onResult called with: $id")
                capturedId = id
            }
        )

        advanceUntilIdle()
        println("Finished waiting success. capturedId: $capturedId")
        
        assertTrue("Expected positive session ID, got $capturedId", capturedId > 0)
    }

    @Test
    fun `viewModel startParkingSession propagates failure result`() = runTest {
        println("Starting viewModel failure test")
        val restrictedScan = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            statusChipText = "No Parking",
            allowedUntilTime = "Restriction active",
            timeRemaining = "0m",
            parkingRules = listOf("No Parking Any Time"),
            explanation = "Invalid for timer",
            isDemo = false
        )
        val scanId = repository.saveScan(restrictedScan)
        println("Saved scan for VM failure test: $scanId")

        var capturedId = -2L
        viewModel.startParkingSession(
            scanResultId = scanId,
            scanResult = restrictedScan,
            locationName = "Restricted Spot",
            durationMinutes = 60,
            onResult = { id -> 
                println("onResult called with: $id")
                capturedId = id
            }
        )

        advanceUntilIdle()
        println("Finished waiting failure. capturedId: $capturedId")
        
        assertEquals(-1L, capturedId)
    }
}


