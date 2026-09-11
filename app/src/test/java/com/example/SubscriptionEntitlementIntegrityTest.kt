package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionPreferences
import com.example.data.remote.SubscriptionService
import com.example.viewmodel.CurbViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionEntitlementIntegrityTest {

    private lateinit var app: Application
    private lateinit var sessionPreferences: SessionPreferences

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        sessionPreferences = SessionPreferences(app)
        sessionPreferences.clearSession()
    }

    @Test
    fun `test matrix 1 - RevenueCat Pro active yields effective Pro true`() = runBlocking {
        // Save profile with active Pro before ViewModel instantiation
        val profile = sessionPreferences.getUserProfile().copy(isPro = true)
        sessionPreferences.saveUserProfile(profile)

        val viewModel = CurbViewModel(app)
        ShadowLooper.idleMainLooper()

        assertTrue("Effective Pro must be true when RevenueCat Pro is active", viewModel.isUserPro())
        assertTrue("Scan usage info must reflect Pro", viewModel.scanUsageInfo.value.isPro)
        assertTrue("Chat usage info must reflect Pro", viewModel.chatUsageInfo.value.isPro)
        assertTrue("canPerformScan must return true", viewModel.canPerformScan())
        assertTrue("canSendChatMessage must return true", viewModel.canSendChatMessage())
    }

    @Test
    fun `test matrix 2 - RevenueCat Pro inactive and no judge unlock yields effective Pro false`() = runBlocking {
        sessionPreferences.isJudgeProActive = false
        sessionPreferences.saveUserProfile(sessionPreferences.getUserProfile().copy(isPro = false))

        val viewModel = CurbViewModel(app)
        ShadowLooper.idleMainLooper()

        assertFalse("Effective Pro must be false when neither paid Pro nor judge unlock is active", viewModel.isUserPro())
        assertFalse("Scan usage info must not reflect Pro", viewModel.scanUsageInfo.value.isPro)
        assertFalse("Chat usage info must not reflect Pro", viewModel.chatUsageInfo.value.isPro)
    }

    @Test
    fun `test matrix 3 - RevenueCat Pro inactive with judge unlock yields effective Pro true`() = runBlocking {
        sessionPreferences.saveUserProfile(sessionPreferences.getUserProfile().copy(isPro = false))

        val viewModel = CurbViewModel(app)
        viewModel.applyPromoCode("CURB26X")
        ShadowLooper.idleMainLooper()

        assertTrue("Effective Pro must be true when judge demo CURB26X is active", viewModel.isUserPro())
        assertTrue("Judge promo flag must be active", viewModel.isJudgeProActive.value)
        assertFalse("UserProfile isPro must remain false to avoid fake store purchase claim", viewModel.userProfile.value.isPro)
        assertTrue("Scan usage info must reflect Pro", viewModel.scanUsageInfo.value.isPro)
        assertTrue("Chat usage info must reflect Pro", viewModel.chatUsageInfo.value.isPro)
    }

    @Test
    fun `test matrix 4 - RevenueCat Pro active with judge unlock yields effective Pro true`() = runBlocking {
        sessionPreferences.saveUserProfile(sessionPreferences.getUserProfile().copy(isPro = true))
        sessionPreferences.isJudgeProActive = true

        val viewModel = CurbViewModel(app)
        ShadowLooper.idleMainLooper()

        assertTrue("Effective Pro must be true when both paid Pro and judge promo are active", viewModel.isUserPro())
    }

    @Test
    fun `test judge unlock survives RevenueCat refresh reporting inactive purchase`() = runBlocking {
        val viewModel = CurbViewModel(app)
        viewModel.applyPromoCode("CURB26X")
        ShadowLooper.idleMainLooper()

        assertTrue("Judge promo active", viewModel.isUserPro())

        // Simulate RevenueCat refresh reporting no active purchase (e.g. isProActive = false)
        val service = SubscriptionService(app)
        service.initialize { isProActive ->
            val updated = sessionPreferences.getUserProfile().copy(isPro = isProActive)
            sessionPreferences.saveUserProfile(updated)
            viewModel.refreshUsageInfo()
        }
        ShadowLooper.idleMainLooper()

        assertTrue("Judge unlock must survive RevenueCat refresh reporting false", viewModel.isUserPro())
        assertTrue("Judge promo flag must still be active", viewModel.isJudgeProActive.value)
    }

    @Test
    fun `test logout and guest transition does not corrupt entitlement state`() = runBlocking {
        val viewModel = CurbViewModel(app)
        viewModel.applyPromoCode("CURB26X")
        ShadowLooper.idleMainLooper()

        assertTrue("Judge promo active before logout", viewModel.isUserPro())

        var logoutDone = false
        viewModel.logout { logoutDone = true }
        ShadowLooper.idleMainLooper()

        assertTrue("Logout complete callback fired", logoutDone)
        assertTrue("Judge promo must survive logout", viewModel.isJudgeProActive.value)
        assertTrue("Effective Pro must remain true after logout", viewModel.isUserPro())
    }
}
