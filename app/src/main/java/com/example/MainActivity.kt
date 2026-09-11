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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
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
import com.example.data.model.CurbNote
import com.example.ui.navigation.Routes
import com.example.ui.screens.AboutCurbScreen
import com.example.ui.screens.AccountInfoScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AskCurbScreen
import com.example.ui.screens.CurbProPaywallScreen
import com.example.ui.screens.FindMyCarScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NameSetupScreen
import com.example.ui.screens.NotificationSettingsScreen
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
        com.example.notification.ParkingNotificationScheduler.createNotificationChannel(applicationContext)
        enableEdgeToEdge()
        setContent {
            CurbTheme {
                CurbApp(intent = intent)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
        route = Routes.ASK_CURB,
        label = "Curb AI",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
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
    intent: android.content.Intent? = null,
    viewModel: CurbViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(intent) {
        val targetRoute = intent?.getStringExtra(com.example.notification.ParkingNotificationScheduler.EXTRA_NAVIGATE_ROUTE)
        val sessionId = intent?.getLongExtra(com.example.notification.ParkingNotificationScheduler.EXTRA_SESSION_ID, -1L) ?: -1L
        if (targetRoute == Routes.PARKING_TIMER) {
            if (sessionId > 0) {
                viewModel.loadSessionById(sessionId)
            }
            navController.navigate(Routes.PARKING_TIMER)
        }
    }

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val targetSession by viewModel.targetSession.collectAsStateWithLifecycle()
    val inAppNotifications by viewModel.inAppNotifications.collectAsStateWithLifecycle()
    val hasUnreadNotifications by viewModel.hasUnreadNotifications.collectAsStateWithLifecycle()
    val savedParkingSpot by viewModel.savedParkingSpot.collectAsStateWithLifecycle()
    val demoSavedParkingSpot by viewModel.demoSavedParkingSpot.collectAsStateWithLifecycle()
    val isSavingParkingSpot by viewModel.isSavingParkingSpot.collectAsStateWithLifecycle()
    val parkingSpotSaveError by viewModel.parkingSpotSaveError.collectAsStateWithLifecycle()
    val userLocationState by viewModel.userLocationState.collectAsStateWithLifecycle()
    val recentScans by viewModel.allScans.collectAsStateWithLifecycle()
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val currentScanResult by viewModel.currentScanResult.collectAsStateWithLifecycle()
    val isProcessingScan by viewModel.isProcessingScan.collectAsStateWithLifecycle()
    val processingStatusText by viewModel.processingStatusText.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val isJudgeProActive by viewModel.isJudgeProActive.collectAsStateWithLifecycle()
    val scanUsageInfo by viewModel.scanUsageInfo.collectAsStateWithLifecycle()
    val chatUsageInfo by viewModel.chatUsageInfo.collectAsStateWithLifecycle()
    val walkingRoute by viewModel.walkingRouteState.collectAsStateWithLifecycle()

    val isUserPro = userProfile.isPro || subscriptionState.isPro || isJudgeProActive

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Scan,
        BottomNavItem.Activity,
        BottomNavItem.You
    )

    val showBottomBar = currentRoute in listOf(
        Routes.HOME,
        Routes.ASK_CURB,
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
                        savedParkingSpot = savedParkingSpot,
                        recentScans = recentScans,
                        usageInfo = scanUsageInfo,
                        userLocationResult = userLocationState,
                        onScanClicked = {
                            navController.navigate(Routes.SCAN)
                        },
                        onParkingTimerClicked = {
                            navController.navigate(Routes.PARKING_TIMER)
                        },
                        onFindMyCarClicked = {
                            navController.navigate(Routes.FIND_MY_CAR)
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
                        hasUnreadNotifications = hasUnreadNotifications,
                        onProfileClicked = {
                            navController.navigate(Routes.YOU)
                        },
                        onUpgradeToProClicked = {
                            navController.navigate(Routes.CURB_PRO_PAYWALL)
                        }
                    )
                }

                // 06. SCAN
                composable(Routes.SCAN) {
                    ScanScreen(
                        isProcessing = isProcessingScan,
                        processingStatusText = processingStatusText,
                        usageInfo = scanUsageInfo,
                        isPro = isUserPro,
                        onCaptureImage = { bitmap, boxes ->
                            viewModel.processCapturedImage(
                                bitmap = bitmap,
                                detectionBoxes = boxes,
                                onPaywallRequired = {
                                    Toast.makeText(
                                        context,
                                        "You've used all 10 free scans for this month. Upgrade to Curb Pro for unlimited scans!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate(Routes.CURB_PRO_PAYWALL)
                                },
                                onComplete = {
                                    navController.navigate(Routes.SCAN_OUTPUT)
                                }
                            )
                        },
                        onPresetSelected = { preset ->
                            viewModel.processPresetSign(
                                preset = preset,
                                onPaywallRequired = {
                                    Toast.makeText(
                                        context,
                                        "You've used all 10 free scans for this month. Upgrade to Curb Pro for unlimited scans!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate(Routes.CURB_PRO_PAYWALL)
                                },
                                onComplete = {
                                    navController.navigate(Routes.SCAN_OUTPUT)
                                }
                            )
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // 07. SCAN OUTPUT
                composable(Routes.SCAN_OUTPUT) {
                    val currentScan = currentScanResult ?: recentScans.firstOrNull { !it.isDemo } ?: com.example.data.model.ScanResult(
                        locationName = "Location unavailable",
                        verdict = com.example.data.model.ScanVerdict.AMBIGUOUS,
                        statusChipText = "Signage unclear",
                        allowedUntilTime = "Verify physical signage",
                        timeRemaining = "--",
                        parkingRules = listOf("No verified parking rule has been established."),
                        explanation = "No active scan result available. Please capture a parking sign photo.",
                        detectedSigns = emptyList(),
                        zoneType = "Parking zone",
                        paymentInfo = "",
                        vehicleApplicability = ""
                    )
                    val currentScanNote = allNotes.firstOrNull {
                        it.targetType == com.example.data.model.CurbNote.TARGET_SCAN_RESULT && it.targetId == currentScan.id
                    }

                    ScanOutputScreen(
                        scanResult = currentScan,
                        note = currentScanNote,
                        isPro = isUserPro,
                        onSaveNote = { text ->
                            viewModel.saveNote(com.example.data.model.CurbNote.TARGET_SCAN_RESULT, currentScan.id, text)
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteNote = {
                            viewModel.deleteNote(com.example.data.model.CurbNote.TARGET_SCAN_RESULT, currentScan.id)
                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                        },
                        onViewDetails = {
                            navController.navigate(Routes.PARKING_DETAILS)
                        },
                        onStartParkingSession = { durationMins, allowedUntil, basis, rules ->
                            viewModel.startParkingSession(
                                scanResultId = currentScan.id,
                                locationName = currentScan.locationName,
                                durationMinutes = durationMins,
                                allowedUntilTime = allowedUntil,
                                timerBasis = basis,
                                parkingRuleSummary = rules
                            )
                            Toast.makeText(context, "Parking session started!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Routes.PARKING_TIMER) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        },
                        onAskCurb = {
                            navController.navigate(Routes.ASK_CURB)
                        },
                        onRetake = {
                            navController.popBackStack()
                        },
                        onUpgradeToPro = {
                            navController.navigate(Routes.CURB_PRO_PAYWALL)
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // 08. PARKING DETAILS
                composable(Routes.PARKING_DETAILS) {
                    val currentScan = currentScanResult ?: recentScans.firstOrNull { !it.isDemo } ?: com.example.data.model.ScanResult(
                        locationName = "Location unavailable",
                        verdict = com.example.data.model.ScanVerdict.AMBIGUOUS,
                        statusChipText = "Signage unclear",
                        allowedUntilTime = "Verify physical signage",
                        timeRemaining = "--",
                        parkingRules = listOf("No verified parking rule has been established."),
                        explanation = "No active scan result available. Please capture a parking sign photo.",
                        detectedSigns = emptyList(),
                        zoneType = "Parking zone",
                        paymentInfo = "",
                        vehicleApplicability = ""
                    )
                    val currentScanNote = allNotes.firstOrNull {
                        it.targetType == com.example.data.model.CurbNote.TARGET_SCAN_RESULT && it.targetId == currentScan.id
                    }

                    ParkingDetailsScreen(
                        scanResult = currentScan,
                        note = currentScanNote,
                        isPro = isUserPro,
                        onSaveNote = { text ->
                            viewModel.saveNote(com.example.data.model.CurbNote.TARGET_SCAN_RESULT, currentScan.id, text)
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteNote = {
                            viewModel.deleteNote(com.example.data.model.CurbNote.TARGET_SCAN_RESULT, currentScan.id)
                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                        },
                        onStartParkingSession = { durationMins, allowedUntil, basis, rules ->
                            viewModel.startParkingSession(
                                scanResultId = currentScan.id,
                                locationName = currentScan.locationName,
                                durationMinutes = durationMins,
                                allowedUntilTime = allowedUntil,
                                timerBasis = basis,
                                parkingRuleSummary = rules
                            )
                            Toast.makeText(context, "Parking session started!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Routes.PARKING_TIMER) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        },
                        onReportIssue = {
                            Toast.makeText(context, "Thank you! Parking report submitted for review.", Toast.LENGTH_LONG).show()
                        },
                        onUpgradeToPro = {
                            navController.navigate(Routes.CURB_PRO_PAYWALL)
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
                        scanResult = currentScanResult,
                        usageInfo = chatUsageInfo,
                        isPro = isUserPro,
                        userName = userProfile.name,
                        onSendMessage = { query ->
                            viewModel.sendChatMessage(query)
                        },
                        onUpgradeToPro = {
                            navController.navigate(Routes.CURB_PRO_PAYWALL)
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
                        isPro = isUserPro,
                        onScanClicked = { scan ->
                            viewModel.setCurrentScan(scan)
                            navController.navigate(Routes.SCAN_OUTPUT)
                        },
                        onUpgradeToPro = {
                            navController.navigate(Routes.CURB_PRO_PAYWALL)
                        }
                    )
                }

                // 11. YOU
                composable(Routes.YOU) {
                    YouScreen(
                        userProfile = userProfile,
                        isPro = isUserPro,
                        onAccountInfoClicked = { navController.navigate(Routes.ACCOUNT_INFO) },
                        onNotificationsClicked = { navController.navigate(Routes.NOTIFICATION_SETTINGS) },
                        onPaymentSubscriptionClicked = { navController.navigate(Routes.PAYMENT_SUBSCRIPTION) },
                        onHelpSupportClicked = { navController.navigate(Routes.HELP_SUPPORT) },
                        onAboutCurbClicked = { navController.navigate(Routes.ABOUT_CURB) },
                        onLogoutClicked = {
                            viewModel.logout {
                                Toast.makeText(context, "Logged out.", Toast.LENGTH_SHORT).show()
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
                            viewModel.deleteAccount {
                                Toast.makeText(context, "Account deleted & data cleared.", Toast.LENGTH_SHORT).show()
                                navController.navigate(Routes.WELCOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 13. NOTIFICATIONS (In-App Notification Center)
                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(
                        notifications = inAppNotifications,
                        onNotificationClicked = { sessionId ->
                            viewModel.loadSessionById(sessionId)
                            navController.navigate(Routes.PARKING_TIMER)
                        },
                        onOpenSettings = {
                            navController.navigate(Routes.NOTIFICATION_SETTINGS)
                        },
                        onMarkRead = {
                            viewModel.markNotificationsAsRead()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 13B. NOTIFICATION SETTINGS
                composable(Routes.NOTIFICATION_SETTINGS) {
                    NotificationSettingsScreen(
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
                        isPro = isUserPro,
                        usageInfo = scanUsageInfo,
                        subscriptionState = subscriptionState,
                        onUpgradeToPro = {
                            navController.navigate(Routes.CURB_PRO_PAYWALL)
                        },
                        onRestorePurchases = { onResult ->
                            viewModel.restoreSubscriptionPurchases { success, msg ->
                                onResult(success, msg)
                            }
                        },
                        onApplyPromoCode = { code ->
                            viewModel.applyPromoCode(code)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 15. CURB PRO PAYWALL
                composable(Routes.CURB_PRO_PAYWALL) {
                    CurbProPaywallScreen(
                        subscriptionState = subscriptionState,
                        onPurchase = { activity, pkg, onSuccess, onError ->
                            viewModel.purchaseSubscription(
                                activity = activity,
                                packageInfo = pkg,
                                onSuccess = {
                                    onSuccess()
                                },
                                onError = { err ->
                                    onError(err)
                                }
                            )
                        },
                        onRestorePurchases = { onResult ->
                            viewModel.restoreSubscriptionPurchases { success, msg ->
                                onResult(success, msg)
                            }
                        },
                        onApplyPromoCode = { code ->
                            viewModel.applyPromoCode(code)
                        },
                        onTermsClicked = { navController.navigate(Routes.TERMS_OF_SERVICE) },
                        onPrivacyClicked = { navController.navigate(Routes.PRIVACY_POLICY) },
                        onDismiss = { navController.popBackStack() }
                    )
                }

                // 16. HELP & SUPPORT
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

                // 17. PRIVACY POLICY
                composable(Routes.PRIVACY_POLICY) {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }

                // 18. TERMS OF SERVICE
                composable(Routes.TERMS_OF_SERVICE) {
                    TermsOfServiceScreen(onBack = { navController.popBackStack() })
                }

                // 19. ABOUT CURB
                composable(Routes.ABOUT_CURB) {
                    AboutCurbScreen(onBack = { navController.popBackStack() })
                }

                // 20. SAVED PLACES
                composable(Routes.SAVED_PLACES) {
                    SavedPlacesScreen(
                        savedPlaces = savedPlaces,
                        notes = allNotes,
                        isPro = isUserPro,
                        onAddPlace = { name, address, note ->
                            viewModel.addSavedPlace(name, address, note)
                            Toast.makeText(context, "Place saved", Toast.LENGTH_SHORT).show()
                        },
                        onDeletePlace = { id ->
                            viewModel.deleteSavedPlace(id)
                            Toast.makeText(context, "Place removed", Toast.LENGTH_SHORT).show()
                        },
                        onSaveNote = { targetType, targetId, text ->
                            viewModel.saveNote(targetType, targetId, text)
                            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteNote = { targetType, targetId ->
                            viewModel.deleteNote(targetType, targetId)
                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                        },
                        onScanPlace = { place ->
                            viewModel.processCapturedImage(
                                bitmap = null,
                                explicitLocationName = place.name,
                                explicitCityState = place.address,
                                onPaywallRequired = {
                                    Toast.makeText(
                                        context,
                                        "You've used all 10 free scans for this month. Upgrade to Curb Pro for unlimited scans!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate(Routes.CURB_PRO_PAYWALL)
                                },
                                onComplete = {
                                    navController.navigate(Routes.SCAN_OUTPUT)
                                }
                            )
                        },
                        onUpgradeToPro = {
                            navController.navigate(Routes.CURB_PRO_PAYWALL)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // 21. PARKING TIMER
                composable(Routes.PARKING_TIMER) {
                    val effectiveSpotForTimer = if (activeSession?.isDemo == true) demoSavedParkingSpot else savedParkingSpot
                    ParkingTimerScreen(
                        activeSession = activeSession,
                        targetSession = targetSession,
                        savedParkingSpot = effectiveSpotForTimer,
                        isSavingParkingSpot = isSavingParkingSpot,
                        parkingSpotSaveError = parkingSpotSaveError,
                        onSaveParkingSpot = {
                            viewModel.saveCurrentParkingSpot(sessionId = activeSession?.id) { success, error ->
                                if (success) {
                                    Toast.makeText(context, "Parking spot saved", Toast.LENGTH_SHORT).show()
                                } else if (!error.isNullOrBlank()) {
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onNavigateToFindMyCar = {
                            navController.navigate(Routes.FIND_MY_CAR)
                        },
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
                        onBack = {
                            viewModel.clearTargetSession()
                            navController.popBackStack()
                        }
                    )
                }

                // 22. FIND MY CAR
                composable(Routes.FIND_MY_CAR) {
                    FindMyCarScreen(
                        savedParkingSpot = savedParkingSpot,
                        userLocationState = userLocationState,
                        walkingRoute = walkingRoute,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onRefreshLocation = {
                            viewModel.refreshLocation()
                        },
                        onStartLiveTracking = {
                            viewModel.startLiveLocationUpdates()
                        },
                        onStopLiveTracking = {
                            viewModel.stopLiveLocationUpdates()
                        },
                        onUpdateWalkingRoute = { uLat, uLng, cLat, cLng ->
                            viewModel.updateWalkingRouteIfNeeded(uLat, uLng, cLat, cLng)
                        },
                        onNavigateToParkingTimer = {
                            navController.navigate(Routes.PARKING_TIMER)
                        }
                    )
                }
            }
        }
    }
}
