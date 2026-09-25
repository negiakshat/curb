package com.example

import com.example.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationHierarchyTest {

    @Test
    fun `verify top level destinations`() {
        val topLevelRoutes = listOf(
            Routes.HOME,
            Routes.ASK_CURB,
            Routes.ACTIVITY,
            Routes.YOU
        )
        assertEquals(4, topLevelRoutes.size)
        assertTrue(topLevelRoutes.contains(Routes.HOME))
        assertTrue(topLevelRoutes.contains(Routes.ASK_CURB))
        assertTrue(topLevelRoutes.contains(Routes.ACTIVITY))
        assertTrue(topLevelRoutes.contains(Routes.YOU))
    }

    @Test
    fun `verify core parking workflow routes exist`() {
        val coreFlowRoutes = listOf(
            Routes.HOME,
            Routes.SCAN,
            Routes.SCAN_OUTPUT,
            Routes.PARKING_DETAILS,
            Routes.PARKING_TIMER,
            Routes.FIND_MY_CAR
        )
        assertEquals(6, coreFlowRoutes.size)
    }

    @Test
    fun `verify secondary utility destinations exist`() {
        val secondaryRoutes = listOf(
            Routes.ACCOUNT_INFO,
            Routes.NOTIFICATIONS,
            Routes.NOTIFICATION_SETTINGS,
            Routes.PAYMENT_SUBSCRIPTION,
            Routes.CURB_PRO_PAYWALL,
            Routes.HELP_SUPPORT,
            Routes.PRIVACY_POLICY,
            Routes.TERMS_OF_SERVICE,
            Routes.ABOUT_CURB,
            Routes.SAVED_PLACES
        )
        assertEquals(10, secondaryRoutes.size)
    }

    @Test
    fun `verify all route values are distinct`() {
        val allRoutes = listOf(
            Routes.SPLASH,
            Routes.WELCOME,
            Routes.NAME_SETUP,
            Routes.PERMISSIONS,
            Routes.HOME,
            Routes.SCAN,
            Routes.SCAN_OUTPUT,
            Routes.PARKING_DETAILS,
            Routes.ASK_CURB,
            Routes.ACTIVITY,
            Routes.YOU,
            Routes.ACCOUNT_INFO,
            Routes.NOTIFICATIONS,
            Routes.NOTIFICATION_SETTINGS,
            Routes.PAYMENT_SUBSCRIPTION,
            Routes.CURB_PRO_PAYWALL,
            Routes.HELP_SUPPORT,
            Routes.PRIVACY_POLICY,
            Routes.TERMS_OF_SERVICE,
            Routes.ABOUT_CURB,
            Routes.SAVED_PLACES,
            Routes.PARKING_TIMER,
            Routes.FIND_MY_CAR
        )
        assertEquals(allRoutes.size, allRoutes.toSet().size)
    }
}
