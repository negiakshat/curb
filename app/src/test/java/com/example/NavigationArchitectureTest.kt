package com.example

import com.example.ui.navigation.Routes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationArchitectureTest {

    @Test
    fun `verify CurbNavGraph composable function exists`() {
        val navGraphClass = try {
            Class.forName("com.example.ui.navigation.CurbNavGraphKt")
        } catch (e: ClassNotFoundException) {
            null
        }
        assertNotNull("CurbNavGraphKt class should exist", navGraphClass)
    }

    @Test
    fun `verify MainActivity delegates navigation to CurbNavGraph and has no raw NavHost or duplicate composables`() {
        val mainActivityFile = File("src/main/java/com/example/MainActivity.kt")
        val curbNavGraphFile = File("src/main/java/com/example/ui/navigation/CurbNavGraph.kt")

        if (mainActivityFile.exists() && curbNavGraphFile.exists()) {
            val mainActivityText = mainActivityFile.readText()
            val navGraphText = curbNavGraphFile.readText()

            assertTrue("MainActivity should call CurbNavGraph", mainActivityText.contains("CurbNavGraph("))
            assertFalse("MainActivity should not contain NavHost directly", mainActivityText.contains("NavHost("))
            assertFalse("MainActivity should not declare composable routes", mainActivityText.contains("composable(Routes."))

            assertTrue("CurbNavGraph should contain NavHost", navGraphText.contains("NavHost("))
            assertTrue("CurbNavGraph should declare SPLASH route", navGraphText.contains("composable(Routes.SPLASH)"))
            assertTrue("CurbNavGraph should declare HOME route", navGraphText.contains("composable(Routes.HOME)"))
            assertTrue("CurbNavGraph should declare SCAN route", navGraphText.contains("composable(Routes.SCAN)"))
            assertTrue("CurbNavGraph should declare PARKING_TIMER route", navGraphText.contains("composable(Routes.PARKING_TIMER)"))
            assertTrue("CurbNavGraph should declare FIND_MY_CAR route", navGraphText.contains("composable(Routes.FIND_MY_CAR)"))
            assertTrue("CurbNavGraph should declare CONTEXTUAL_COPILOT route", navGraphText.contains("composable(Routes.CONTEXTUAL_COPILOT)"))
        }
    }

    @Test
    fun `verify all core routes are declared in CurbNavGraph`() {
        val curbNavGraphFile = File("src/main/java/com/example/ui/navigation/CurbNavGraph.kt")
        if (curbNavGraphFile.exists()) {
            val navGraphText = curbNavGraphFile.readText()

            val expectedRoutes = listOf(
                "Routes.SPLASH",
                "Routes.WELCOME",
                "Routes.NAME_SETUP",
                "Routes.PERMISSIONS",
                "Routes.HOME",
                "Routes.SCAN",
                "Routes.SCAN_OUTPUT",
                "Routes.PARKING_DETAILS",
                "Routes.CONTEXTUAL_COPILOT",
                "Routes.ACTIVITY",
                "Routes.YOU",
                "Routes.ACCOUNT_INFO",
                "Routes.NOTIFICATIONS",
                "Routes.NOTIFICATION_SETTINGS",
                "Routes.PAYMENT_SUBSCRIPTION",
                "Routes.CURB_PRO_PAYWALL",
                "Routes.HELP_SUPPORT",
                "Routes.PRIVACY_POLICY",
                "Routes.TERMS_OF_SERVICE",
                "Routes.ABOUT_CURB",
                "Routes.SAVED_PLACES",
                "Routes.PARKING_TIMER",
                "Routes.FIND_MY_CAR"
            )

            for (route in expectedRoutes) {
                assertTrue("Nav graph missing route $route", navGraphText.contains("composable($route)"))
            }
        }
    }
}
