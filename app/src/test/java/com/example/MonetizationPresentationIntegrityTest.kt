package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.SubscriptionService
import com.example.viewmodel.CurbViewModel
import com.example.viewmodel.PromoCodeResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
class MonetizationPresentationIntegrityTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `verify standard fallback pricing packages match explicit tier pricing`() {
        val packages = SubscriptionService.DEFAULT_PACKAGES
        assertEquals(3, packages.size)

        val weekly = packages.find { it.id == "weekly" }
        assertEquals("$2.99", weekly?.priceString)
        assertEquals("/ week", weekly?.period)
        assertEquals(SubscriptionService.PRODUCT_WEEKLY, weekly?.productId)

        val monthly = packages.find { it.id == "monthly" }
        assertEquals("$4.99", monthly?.priceString)
        assertEquals("/ month", monthly?.period)
        assertEquals(SubscriptionService.PRODUCT_MONTHLY, monthly?.productId)

        val annual = packages.find { it.id == "annual" }
        assertEquals("$29.99", annual?.priceString)
        assertEquals("/ year", annual?.period)
        assertEquals(SubscriptionService.PRODUCT_ANNUAL, annual?.productId)
        assertTrue(annual?.isBestValue == true)
    }

    @Test
    fun `verify CURB26X promo code yields PromoCodeResult Success and distinguishes Judge Demo`() = runBlocking {
        val viewModel = CurbViewModel(app)
        ShadowLooper.idleMainLooper()

        val result = viewModel.applyPromoCode("CURB26X")
        assertTrue("CURB26X promo code application must return Success", result is PromoCodeResult.Success)

        assertTrue("ViewModel isUserPro() must be true after CURB26X", viewModel.isUserPro())
        assertTrue("isJudgeProActive must be true after CURB26X", viewModel.isJudgeProActive.value)
        assertFalse("UserProfile isPro must remain false to accurately distinguish Judge Demo from store subscription", viewModel.userProfile.value.isPro)
    }

    @Test
    fun `verify invalid promo codes return PromoCodeResult Error`() = runBlocking {
        val viewModel = CurbViewModel(app)
        ShadowLooper.idleMainLooper()

        val result = viewModel.applyPromoCode("INVALID_123")
        assertTrue("Invalid promo code application must return Error", result is PromoCodeResult.Error)
        assertEquals("Invalid promo code.", (result as PromoCodeResult.Error).message)
    }
}
