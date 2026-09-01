package com.example

import android.os.Bundle
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Routes
import com.example.ui.screens.AboutCurbScreen
import com.example.ui.screens.AccountInfoScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AskCurbScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NameSetupScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ParkingDetailsScreen
import com.example.ui.screens.ParkingTimerScreen
import com.example.ui.screens.PaymentSubscriptionScreen
import com.example.ui.screens.PermissionsFlowScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SavedPlacesScreen
import com.example.ui.screens.ScanOutputScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TermsOfServiceScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.YouScreen
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.CurbTheme
import com.example.viewmodel.CurbViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CurbTheme {
                CurbApp()
            }
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : BottomNavItem(
        route = Routes.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    )
    object Scan : BottomNavItem(
        route = Routes.SCAN,
        label = "Scan",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        testTag = "nav_tab_scan"
    )
    object Activity : BottomNavItem(
        route = Routes.ACTIVITY,
        label = "Activity",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_activity"
    )
    object You : BottomNavItem(
        route = Routes.YOU,
        label = "You",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_you"
    )
}

@Composable
fun CurbApp(
    viewModel: CurbViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val recentScans by viewModel.allScans.collectAsStateWithLifecycle()
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val currentScanResult by viewModel.currentScanResult.collectAsStateWithLifecycle()
    val isProcessingScan by viewModel.isProcessingScan.collectAsStateWithLifecycle()
    val processingStatusText by viewModel.processingStatusText.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Scan,
        BottomNavItem.Activity,
        BottomNavItem.You
    )

    val showBottomBar = currentRoute in listOf(
        Routes.HOME,
        Routes.ACTIVITY,
        Routes.YOU
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                NavigationBar(
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = BentoBorder,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                    containerColor = BentoCanvas,
                    contentColor = BentoPrimaryDark,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.4.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BentoPrimaryDark,
                                selectedTextColor = BentoPrimaryDark,
                                unselectedIconColor = BentoTextSecondary,
                                unselectedTextColor = BentoTextSecondary,
                                indicatorColor = BentoPeach
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH
            ) {
                // 01. SPLASH
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onSplashFinished = {
                            if (viewModel.isOnboardingAndPermissionsCompleted()) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Routes.WELCOME) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // 02. WELCOME
                composable(Routes.WELCOME) {
                    WelcomeScreen(
                        onGetStarted = {
                            navController.navigate(Routes.NAME_SETUP)
                        },
                        onContinueAsGuest = {
                            viewModel.startGuestSession {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            }
                        },
                        onTermsClicked = {
                            navController.navigate(Routes.TERMS_OF_SERVICE)
                        },
                        onPrivacyClicked = {
                            navController.navigate(Routes.PRIVACY_POLICY)
                        }
                    )
                }

                // 03. NAME SETUP
                composable(Routes.NAME_SETUP) {
                    NameSetupScreen(
                        currentName = userProfile.name,
                        onNameSubmitted = { name ->
                            viewModel.setUserName(name)
                            navController.navigate(Routes.PERMISSIONS)
                        }
                    )
                }

                // 04. PERMISSIONS FLOW
                composable(Routes.PERMISSIONS) {
                    PermissionsFlowScreen(
                        onPermissionsFinished = {
                            viewModel.completeOnboarding()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }
                    )
                }

                // 05. HOME
                composable(Routes.HOME) {
                    HomeScreen(
                        userProfile = userProfile,
                        activeSession = activeSession,
                        recentScans = recentScans,
                        onScanClicked = {
                            navController.navigate(Routes.SCAN)
                        },
                        onParkingTimerClicked = {
                            navController.navigate(Routes.PARKING_TIMER)
                        },
                        onSavedPlacesClicked = {
                            navController.navigate(Routes.SAVED_PLACES)
                        },
                        onActivityClicked = {
                            navController.navigate(Routes.ACTIVITY)
                        },
                        onAskCurbClicked = {
                            navController.navigate(Routes.ASK_CURB)
                        },
                        onScanResultClicked = { scan ->
                            viewModel.setCurrentScan(scan)
                            navController.navigate(Routes.SCAN_OUTPUT)
                        },
                        onNotificationsClicked = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        onProfileClicked = {
                            navController.navigate(Routes.YOU)
                        }
                    )
                }

                // 06. SCAN
                composable(Routes.SCAN) {
                    ScanScreen(
                        isProcessing = isProcessingScan,
                        processingStatusText = processingStatusText,
                        onCaptureImage = { bitmap ->
                            viewModel.processCapturedImage(bitmap, locationName = "Mission Street") {
                                navController.navigate(Routes.SCAN_OUTPUT)
                            }
                        },
                        onPresetSelected = { preset ->
                            viewModel.processPresetSign(preset) {
                                navController.navigate(Routes.SCAN_OUTPUT)
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // 07. SCAN OUTPUT
                composable(Routes.SCAN_OUTPUT) {
                    val currentScan = currentScanResult ?: recentScans.firstOrNull() ?: viewModel.userProfile.value.let {
                        // Fallback result
                        com.example.data.remote.GeminiService.generateIntelligentScanResult("Mission Street")
                    }

                    ScanOutputScreen(
                        scanResult = currentScan,
                        onViewDetails = {
                            navController.navigate(Routes.PARKING_DETAILS)
                        },
                        onStartParkingSession = {
                            viewModel.startParkingSession(
                                scanResultId = currentScan.id,
                                locationName = currentScan.locationName,
                                durationMinutes = 135,
                                allowedUntilTime = currentScan.allowedUntilTime
                            )
                            Toast.makeText(context, "Parking session started!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onAskCurb = {
                            navController.navigate(Routes.ASK_CURB)
                        },
                        onRetake = {
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // 08. PARKING DETAILS
                composable(Routes.PARKING_DETAILS) {
                    val currentScan = currentScanResult ?: recentScans.firstOrNull() ?: com.example.data.remote.GeminiService.generateIntelligentScanResult("Mission Street")

                    ParkingDetailsScreen(
                        scanResult = currentScan,
                        onStartParkingSession = {
                            viewModel.startParkingSession(
                                scanResultId = currentScan.id,
                                locationName = currentScan.locationName,
                                durationMinutes = 135,
                                allowedUntilTime = currentScan.allowedUntilTime
                            )
                            Toast.makeText(context, "Parking session started!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onReportIssue = {
                            Toast.makeText(context, "Thank you! Parking report submitted for review.", Toast.LENGTH_LONG).show()
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // 09. ASK CURB AI
                composable(Routes.ASK_CURB) {
                    AskCurbScreen(
                        messages = chatMessages,
                        isLoading = isChatLoading,
                        onSendMessage = { query ->
                            viewModel.sendChatMessage(query)
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // 10. ACTIVITY
                composable(Routes.ACTIVITY) {
                    ActivityScreen(
                        scans = recentScans,
                        onScanClicked = { scan ->
                            viewModel.setCurrentScan(scan)
                            navController.navigate(Routes.SCAN_OUTPUT)
                        }
                    )
                }

                // 11. YOU
                composable(Routes.YOU) {
                    YouScreen(
                        userProfile = userProfile,
                        onAccountInfoClicked = { navController.navigate(Routes.ACCOUNT_INFO) },
                        onNotificationsClicked = { navController.navigate(Routes.NOTIFICATIONS) },
                        onPaymentSubscriptionClicked = { navController.navigate(Routes.PAYMENT_SUBSCRIPTION) },
                        onHelpSupportClicked = { navController.navigate(Routes.HELP_SUPPORT) },
                        onAboutCurbClicked = { navController.navigate(Routes.ABOUT_CURB) },
                        onLogoutClicked = {
                            viewModel.logout {
                                Toast.makeText(context, "Logged out. Session cleared.", Toast.LENGTH_SHORT).show()
                                navController.navigate(Routes.WELCOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // 12. ACCOUNT INFO
                composable(Routes.ACCOUNT_INFO) {
                    AccountInfoScreen(
                        userProfile = userProfile,
                        onSaveProfile = { name, gender, email ->
                            viewModel.updateAccount(name, gender, email)
                            Toast.makeText(context, "Account updated", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteAccount = {
                            viewModel.logout {
                                Toast.makeText(context, "Account deleted & session wiped.", Toast.LENGTH_SHORT).show()
                                navController.navigate(Routes.WELCOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 13. NOTIFICATIONS
                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(
                        pushEnabled = userProfile.pushNotificationsEnabled,
                        onTogglePush = { enabled ->
                            viewModel.togglePushNotifications(enabled)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 14. PAYMENT & SUBSCRIPTION
                composable(Routes.PAYMENT_SUBSCRIPTION) {
                    PaymentSubscriptionScreen(
                        isPro = userProfile.isPro,
                        onUpgradeToPro = {
                            viewModel.upgradeToPro()
                            Toast.makeText(context, "Upgraded to Curb Pro!", Toast.LENGTH_SHORT).show()
                        },
                        onRestorePurchases = {
                            viewModel.restorePurchases()
                            Toast.makeText(context, "Purchases restored", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 15. HELP & SUPPORT
                composable(Routes.HELP_SUPPORT) {
                    HelpSupportScreen(
                        onPrivacyClicked = { navController.navigate(Routes.PRIVACY_POLICY) },
                        onTermsClicked = { navController.navigate(Routes.TERMS_OF_SERVICE) },
                        onContactSupport = {
                            Toast.makeText(context, "Contacting support at support@curbparking.app", Toast.LENGTH_LONG).show()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 16. PRIVACY POLICY
                composable(Routes.PRIVACY_POLICY) {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }

                // 17. TERMS OF SERVICE
                composable(Routes.TERMS_OF_SERVICE) {
                    TermsOfServiceScreen(onBack = { navController.popBackStack() })
                }

                // 18. ABOUT CURB
                composable(Routes.ABOUT_CURB) {
                    AboutCurbScreen(onBack = { navController.popBackStack() })
                }

                // 19. SAVED PLACES
                composable(Routes.SAVED_PLACES) {
                    SavedPlacesScreen(
                        savedPlaces = savedPlaces,
                        onAddPlace = { name, address, note ->
                            viewModel.addSavedPlace(name, address, note)
                            Toast.makeText(context, "Place saved", Toast.LENGTH_SHORT).show()
                        },
                        onDeletePlace = { id ->
                            viewModel.deleteSavedPlace(id)
                            Toast.makeText(context, "Place removed", Toast.LENGTH_SHORT).show()
                        },
                        onScanPlace = { place ->
                            viewModel.processCapturedImage(null, locationName = place.name) {
                                navController.navigate(Routes.SCAN_OUTPUT)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 20. PARKING TIMER
                composable(Routes.PARKING_TIMER) {
                    ParkingTimerScreen(
                        activeSession = activeSession,
                        onStartQuickTimer = { minutes, limitText ->
                            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                            val allowedTime = sdf.format(Date(System.currentTimeMillis() + (minutes * 60 * 1000L)))
                            viewModel.startParkingSession(
                                durationMinutes = minutes,
                                allowedUntilTime = allowedTime,
                                locationName = "Mission Street",
                                notes = "Metered parking • Space #42"
                            )
                            Toast.makeText(context, "Timer started for $minutes minutes", Toast.LENGTH_SHORT).show()
                        },
                        onEndSession = { id ->
                            viewModel.endActiveParkingSession(id)
                            Toast.makeText(context, "Parking session ended", Toast.LENGTH_SHORT).show()
                        },
                        onExtendSession = { id, addMins, currentEndTime ->
                            viewModel.extendParkingSession(id, addMins, currentEndTime)
                        },
                        onUpdateReminder = { id, reminderMins ->
                            viewModel.updateSessionReminder(id, reminderMins)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
