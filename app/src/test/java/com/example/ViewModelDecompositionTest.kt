package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatUsageManager
import com.example.data.local.ScanUsageManager
import com.example.data.local.SessionPreferences
import com.example.data.location.LocationService
import com.example.data.model.ActiveParkingSession
import com.example.data.model.SampleSignPreset
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.data.remote.SubscriptionService
import com.example.data.repository.CurbRepository
import com.example.viewmodel.CurbViewModel
import com.example.viewmodel.PromoCodeResult
import com.example.viewmodel.coordinators.ChatCoordinator
import com.example.viewmodel.coordinators.LocationSpotCoordinator
import com.example.viewmodel.coordinators.NotificationCoordinator
import com.example.viewmodel.coordinators.ProfileCoordinator
import com.example.viewmodel.coordinators.ScanCoordinator
import com.example.viewmodel.coordinators.UsageCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ViewModelDecompositionTest {

    private lateinit var application: Application
    private lateinit var repository: CurbRepository
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var scanUsageManager: ScanUsageManager
    private lateinit var chatUsageManager: ChatUsageManager
    private lateinit var subscriptionService: SubscriptionService
    private lateinit var locationService: LocationService

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        repository = CurbRepository(application)
        sessionPreferences = SessionPreferences(application)
        scanUsageManager = ScanUsageManager(application)
        chatUsageManager = ChatUsageManager(application)
        subscriptionService = SubscriptionService(application)
        locationService = LocationService(application)
    }

    @Test
    fun `verify ProfileCoordinator promo code and user updates`() = testScope.runTest {
        val coordinator = ProfileCoordinator(
            repository = repository,
            sessionPreferences = sessionPreferences,
            scanUsageManager = scanUsageManager,
            chatUsageManager = chatUsageManager,
            coroutineScope = this
        )

        coordinator.updateAccount(name = "Jordan Miller", gender = "Non-binary", email = "jordan@example.com")
        assertEquals("Jordan Miller", coordinator.userProfile.value.name)
        assertEquals("jordan@example.com", coordinator.userProfile.value.email)

        val promoResult = coordinator.applyPromoCode("CURB26X")
        assertTrue(promoResult is PromoCodeResult.Success)
        assertTrue(coordinator.isJudgeProActive.value)

        val invalidResult = coordinator.applyPromoCode("INVALID")
        assertTrue(invalidResult is PromoCodeResult.Error)
    }

    @Test
    fun `verify UsageCoordinator evaluates pro state and quotas`() = testScope.runTest {
        val judgeProFlow = MutableStateFlow(true)
        val profileFlow = MutableStateFlow(sessionPreferences.getUserProfile())

        val coordinator = UsageCoordinator(
            scanUsageManager = scanUsageManager,
            chatUsageManager = chatUsageManager,
            subscriptionService = subscriptionService,
            sessionPreferences = sessionPreferences,
            isJudgeProActiveFlow = judgeProFlow,
            userProfileFlow = profileFlow,
            coroutineScope = this
        )

        backgroundScope.launch { coordinator.isUserPro.collect {} }
        backgroundScope.launch { coordinator.scanUsageInfo.collect {} }
        backgroundScope.launch { coordinator.chatUsageInfo.collect {} }

        assertTrue(coordinator.isUserPro.value)
        assertTrue(coordinator.canPerformScan())
        assertTrue(coordinator.canSendChatMessage())
    }

    @Test
    fun `verify NotificationCoordinator generates notification list and tracks read status`() = testScope.runTest {
        val now = System.currentTimeMillis()
        val activeFlow = MutableStateFlow<ActiveParkingSession?>(
            ActiveParkingSession(
                id = 101L,
                scanResultId = 1L,
                locationName = "Market St & 4th St",
                startTime = now - 600000L,
                endTime = now + 1800000L,
                allowedUntilTime = "2:00 PM",
                reminderMinutesBefore = 15,
                isActive = true,
                isDemo = false
            )
        )
        val allSessionsFlow = MutableStateFlow<List<ActiveParkingSession>>(emptyList())

        val coordinator = NotificationCoordinator(
            sessionPreferences = sessionPreferences,
            activeSession = activeFlow,
            allSessions = allSessionsFlow,
            coroutineScope = this
        )

        backgroundScope.launch { coordinator.inAppNotifications.collect {} }
        backgroundScope.launch { coordinator.hasUnreadNotifications.collect {} }

        val notifications = coordinator.inAppNotifications.value
        assertEquals(1, notifications.size)
        assertEquals("Market St & 4th St", notifications[0].locationName)
        assertEquals(101L, notifications[0].sessionId)

        coordinator.markNotificationsAsRead()
        assertFalse(coordinator.hasUnreadNotifications.value)
    }

    @Test
    fun `verify ScanCoordinator preset sign processing`() = testScope.runTest {
        val activeLocFlow = MutableStateFlow<com.example.data.location.UserLocationResult>(
            com.example.data.location.UserLocationResult.Success(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 5f,
                timestamp = System.currentTimeMillis(),
                locationName = "Downtown",
                cityState = "San Francisco, CA",
                formattedDisplay = "Downtown, San Francisco, CA"
            )
        )

        val coordinator = ScanCoordinator(
            application = application,
            repository = repository,
            scanUsageManager = scanUsageManager,
            locationService = locationService,
            userLocationState = activeLocFlow,
            coroutineScope = this
        )

        val presets = com.example.data.remote.GeminiService.getPreparedPresets(application)
        val preset = presets.first()
        var completed = false

        coordinator.processPresetSign(
            preset = preset,
            isUserPro = true,
            onComplete = { completed = true }
        )

        testScheduler.advanceUntilIdle()

        assertTrue(completed)
        assertNotNull(coordinator.currentScanResult.value)
        assertEquals(preset.locationName, coordinator.currentScanResult.value?.locationName)

        coordinator.resetScanState()
        assertNull(coordinator.currentScanResult.value)
        assertFalse(coordinator.isProcessingScan.value)
    }

    @Test
    fun `verify ChatCoordinator send message thread`() = testScope.runTest {
        val coordinator = ChatCoordinator(
            chatUsageManager = chatUsageManager,
            sessionPreferences = sessionPreferences,
            coroutineScope = this
        )

        val initialCount = coordinator.chatMessages.value.size
        coordinator.sendChatMessage(
            query = "Can I park here on Sundays?",
            isUserPro = true,
            currentScan = null
        )

        assertTrue(coordinator.chatMessages.value.size > initialCount)
        assertEquals("Can I park here on Sundays?", coordinator.chatMessages.value[initialCount].text)
    }

    @Test
    fun `verify LocationSpotCoordinator error handling when permissions missing`() = testScope.runTest {
        val coordinator = LocationSpotCoordinator(
            repository = repository,
            locationService = locationService,
            coroutineScope = this
        )

        var errorResultMsg: String? = null
        var successFlag = true

        coordinator.saveCurrentParkingSpot(
            sessionId = null,
            isDemo = false,
            onResult = { success, msg ->
                successFlag = success
                errorResultMsg = msg
            }
        )

        assertFalse(successFlag)
        assertNotNull(errorResultMsg)

        coordinator.clearParkingSpotSaveError()
        assertNull(coordinator.parkingSpotSaveError.value)
    }

    @Test
    fun `verify CurbViewModel delegating API`() = testScope.runTest {
        val viewModel = CurbViewModel(application)

        assertNotNull(viewModel.userProfile.value)
        assertNotNull(viewModel.scanUsageInfo.value)
        assertNotNull(viewModel.chatUsageInfo.value)
        assertNotNull(viewModel.inAppNotifications.value)

        viewModel.setUserName("Taylor")
        assertEquals("Taylor", viewModel.userProfile.value.name)

        val promoResult = viewModel.applyPromoCode("CURB26X")
        assertTrue(promoResult is PromoCodeResult.Success)
        assertTrue(viewModel.isUserPro())
    }
}
