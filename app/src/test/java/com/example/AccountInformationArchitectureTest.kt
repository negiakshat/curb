package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.UserProfile
import com.example.ui.screens.YouScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountInformationArchitectureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dummyProfile = UserProfile(
        name = "Test Driver",
        email = "driver@example.com",
        gender = "Prefer not to say",
        isPro = false
    )

    @Test
    fun `verify account info row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = { clicked = true },
                onNotificationsClicked = {},
                onPaymentSubscriptionClicked = {},
                onHelpSupportClicked = {},
                onAboutCurbClicked = {}
            )
        }
        composeTestRule.onNodeWithTag("setting_account_info").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `verify payment subscription row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = {},
                onNotificationsClicked = {},
                onPaymentSubscriptionClicked = { clicked = true },
                onHelpSupportClicked = {},
                onAboutCurbClicked = {}
            )
        }
        composeTestRule.onNodeWithTag("setting_payment_subscription").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `verify notification settings row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = {},
                onNotificationsClicked = { clicked = true },
                onPaymentSubscriptionClicked = {},
                onHelpSupportClicked = {},
                onAboutCurbClicked = {}
            )
        }
        composeTestRule.onNodeWithTag("setting_notifications").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `verify saved places row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = {},
                onNotificationsClicked = {},
                onPaymentSubscriptionClicked = {},
                onHelpSupportClicked = {},
                onAboutCurbClicked = {},
                onSavedPlacesClicked = { clicked = true }
            )
        }
        composeTestRule.onNodeWithTag("setting_saved_places").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `verify help support row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = {},
                onNotificationsClicked = {},
                onPaymentSubscriptionClicked = {},
                onHelpSupportClicked = { clicked = true },
                onAboutCurbClicked = {}
            )
        }
        composeTestRule.onNodeWithTag("setting_help_support").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `verify privacy policy row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = {},
                onNotificationsClicked = {},
                onPaymentSubscriptionClicked = {},
                onHelpSupportClicked = {},
                onAboutCurbClicked = {},
                onPrivacyPolicyClicked = { clicked = true }
            )
        }
        composeTestRule.onNodeWithTag("setting_privacy_policy").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `verify terms of service row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = {},
                onNotificationsClicked = {},
                onPaymentSubscriptionClicked = {},
                onHelpSupportClicked = {},
                onAboutCurbClicked = {},
                onTermsOfServiceClicked = { clicked = true }
            )
        }
        composeTestRule.onNodeWithTag("setting_terms_of_service").performScrollTo().performClick()
        assertTrue(clicked)
    }

    @Test
    fun `verify about curb row is clickable`() {
        var clicked = false
        composeTestRule.setContent {
            YouScreen(
                userProfile = dummyProfile,
                isPro = false,
                onAccountInfoClicked = {},
                onNotificationsClicked = {},
                onPaymentSubscriptionClicked = {},
                onHelpSupportClicked = {},
                onAboutCurbClicked = { clicked = true }
            )
        }
        composeTestRule.onNodeWithTag("setting_about_curb").performScrollTo().performClick()
        assertTrue(clicked)
    }
}

