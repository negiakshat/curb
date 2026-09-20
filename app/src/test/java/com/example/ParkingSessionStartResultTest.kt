package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.repository.CurbRepository
import com.example.viewmodel.CurbViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParkingSessionStartResultTest {

    private lateinit var app: Application
    private lateinit var repository: CurbRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        repository = CurbRepository(app)
        runBlocking {
            repository.clearAllData()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Repository-level result verification ---

    @Test
    fun `repository returns negative when no scan is provided`() = runBlocking {
        val result = repository.startParkingSession(
            durationMinutes = 30,
            locationName = "Some Spot"
        )
        assertTrue("Expected failure (-1) when no scan provided, got $result", result < 0)
    }

    @Test
    fun `repository returns negative for AMBIGUOUS scan`() = runBlocking {
        val ambiguousScan = ScanResult(
            verdict = ScanVerdict.AMBIGUOUS,
            statusChipText = "Rule unclear",
            allowedUntilTime = "Verify physical signage",
            timeRemaining = "--",
            parkingRules = listOf("Sign unreadable"),
            explanation = "Sign text obscured"
        )
        val scanId = repository.saveScan(ambiguousScan)
        val result = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "Ambiguous Spot",
            durationMinutes = 60
        )
        assertEquals(-1L, result)
    }

    @Test
    fun `repository returns negative for RESTRICTED scan`() = runBlocking {
        val restrictedScan = ScanResult(
            verdict = ScanVerdict.RESTRICTED,
            statusChipText = "No Parking",
            allowedUntilTime = "No parking allowed",
            timeRemaining = "0m",
            parkingRules = listOf("No Parking Any Time"),
            explanation = "Active restriction in place"
        )
        val scanId = repository.saveScan(restrictedScan)
        val result = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "Restricted Spot",
            durationMinutes = 60
        )
        assertEquals(-1L, result)
    }

    @Test
    fun `repository returns positive for valid ALLOWED scan with sufficient duration`() = runBlocking {
        val allowedScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Updated just now",
            allowedUntilTime = "2 Hour Parking",
            timeRemaining = "2h 00m remaining",
            parkingRules = listOf("2 Hour Parking 8 AM - 6 PM"),
            explanation = "2 hour limit"
        )
        val scanId = repository.saveScan(allowedScan)
        val result = repository.startParkingSession(
            scanResultId = scanId,
            locationName = "2 Hour Spot",
            durationMinutes = 60
        )
        assertTrue("Expected positive session ID for valid scan, got $result", result > 0)
    }

    // --- ViewModel callback verification ---

    @Test
    fun `ViewModel callback receives negative ID when repository rejects session`() = runBlocking {
        val viewModel = CurbViewModel(app)
        val deferredId = kotlinx.coroutines.CompletableDeferred<Long>()

        viewModel.startParkingSession(
            durationMinutes = 30,
            onResult = { id -> deferredId.complete(id) }
        )
        shadowOf(Looper.getMainLooper()).idle()

        val resultId = kotlinx.coroutines.withTimeout(5000) { deferredId.await() }
        assertTrue("Expected negative callback ID, got $resultId", resultId < 0)
    }

    @Test
    fun `ViewModel callback receives positive ID when repository creates session`() = runBlocking {
        val validScan = ScanResult(
            verdict = ScanVerdict.ALLOWED,
            statusChipText = "Updated just now",
            allowedUntilTime = "1 Hour Parking",
            timeRemaining = "1h 00m remaining",
            parkingRules = listOf("1 Hour Parking 9 AM - 5 PM"),
            explanation = "1 hour limit",
            isDemo = true
        )
        val scanId = repository.saveScan(validScan)

        val viewModel = CurbViewModel(app)
        val deferredId = kotlinx.coroutines.CompletableDeferred<Long>()

        viewModel.startParkingSession(
            scanResultId = scanId,
            scanResult = validScan,
            locationName = "1 Hour Spot",
            durationMinutes = 60,
            onResult = { id -> deferredId.complete(id) }
        )
        shadowOf(Looper.getMainLooper()).idle()

        val resultId = kotlinx.coroutines.withTimeout(5000) { deferredId.await() }
        assertTrue("Expected positive callback ID, got $resultId", resultId > 0)
    }

    @Test
    fun `ViewModel callback defaults to identity when no onResult provided`() {
        // Verify the function doesn't crash when called without onResult
        val viewModel = CurbViewModel(app)
        viewModel.startParkingSession(
            durationMinutes = 30
        )
        shadowOf(Looper.getMainLooper()).idle()
        // No crash = pass. Default lambda {} should handle the result silently.
    }
}
