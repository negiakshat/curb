package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.model.CurbNote
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.screens.AboutCurbScreen
import com.example.ui.screens.AccountInfoScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AskCurbScreen
import com.example.ui.screens.ContextualCopilotScreen
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
import com.example.util.ParkingAuthority
import com.example.util.ParkingTimerCalculator
import com.example.viewmodel.CurbViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun CurbNavGraph(
    navController: NavHostController,
    viewModel: CurbViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.SPLASH
) {
    val context = LocalContext.current

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
    val scanError by viewModel.scanError.collectAsStateWithLifecycle()
    val processingStatusText by viewModel.processingStatusText.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val isJudgeProActive by viewModel.isJudgeProActive.collectAsStateWithLifecycle()
    val scanUsageInfo by viewModel.scanUsageInfo.collectAsStateWithLifecycle()
    val chatUsageInfo by viewModel.chatUsageInfo.collectAsStateWithLifecycle()
    val walkingRoute by viewModel.walkingRouteState.collectAsStateWithLifecycle()
    val isUserPro by viewModel.isUserPro.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
                },
                onBack = {
                    navController.popBackStack()
                },
                canNavigateBack = navController.previousBackStackEntry != null
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
                },
                onBackToNameSetup = {
                    navController.popBackStack()
                }
            )
        }

        // 05. HOME
        composable(Routes.HOME) {
            val effectiveSpotForHome = if (activeSession?.isDemo == true) demoSavedParkingSpot else savedParkingSpot
            HomeScreen(
                userProfile = userProfile,
                isPro = isUserPro,
                activeSession = activeSession,
                savedParkingSpot = effectiveSpotForHome,
                recentScans = recentScans,
                usageInfo = scanUsageInfo,
                userLocationResult = userLocationState,
                onScanClicked = {
                    navController.navigate(Routes.SCAN)
                },
                onParkingTimerClicked = {
                    navController.navigate(Routes.PARKING_TIMER) {
                        launchSingleTop = true
                    }
                },
                onFindMyCarClicked = {
                    navController.navigate(Routes.FIND_MY_CAR) {
                        launchSingleTop = true
                    }
                },
                onSavedPlacesClicked = {
                    navController.navigate(Routes.SAVED_PLACES)
                },
                onActivityClicked = {
                    navController.navigate(Routes.ACTIVITY) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAskCurbClicked = {
                    navController.navigate(Routes.ASK_CURB) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                    navController.navigate(Routes.YOU) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                scanError = scanError,
                onClearScanError = { viewModel.clearScanError() },
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
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
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
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
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
            val currentScan = currentScanResult ?: recentScans.firstOrNull { !it.isDemo } ?: ScanResult(
                locationName = "Location unavailable",
                verdict = ScanVerdict.AMBIGUOUS,
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
                it.targetType == CurbNote.TARGET_SCAN_RESULT && it.targetId == currentScan.id
            }

            ScanOutputScreen(
                scanResult = currentScan,
                note = currentScanNote,
                isPro = isUserPro,
                onSaveNote = { text ->
                    viewModel.saveNote(CurbNote.TARGET_SCAN_RESULT, currentScan.id, text)
                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                },
                onDeleteNote = {
                    viewModel.deleteNote(CurbNote.TARGET_SCAN_RESULT, currentScan.id)
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
                    ) { sessionId ->
                        if (sessionId > 0) {
                            Toast.makeText(context, "Parking session started!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Routes.PARKING_TIMER) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        } else {
                            Toast.makeText(context, "Couldn't start the parking timer. Please scan a valid parking sign first.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onAskCurb = {
                    navController.navigate(Routes.CONTEXTUAL_COPILOT)
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
            val currentScan = currentScanResult ?: recentScans.firstOrNull { !it.isDemo } ?: ScanResult(
                locationName = "Location unavailable",
                verdict = ScanVerdict.AMBIGUOUS,
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
                it.targetType == CurbNote.TARGET_SCAN_RESULT && it.targetId == currentScan.id
            }

            ParkingDetailsScreen(
                scanResult = currentScan,
                note = currentScanNote,
                isPro = isUserPro,
                onSaveNote = { text ->
                    viewModel.saveNote(CurbNote.TARGET_SCAN_RESULT, currentScan.id, text)
                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                },
                onDeleteNote = {
                    viewModel.deleteNote(CurbNote.TARGET_SCAN_RESULT, currentScan.id)
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
                    ) { sessionId ->
                        if (sessionId > 0) {
                            Toast.makeText(context, "Parking session started!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Routes.PARKING_TIMER) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        } else {
                            Toast.makeText(context, "Couldn't start the parking timer. Please scan a valid parking sign first.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onReportIssue = {
                    Toast.makeText(context, "Thank you! Parking report submitted for review.", Toast.LENGTH_LONG).show()
                },
                onAskAboutThisSign = {
                    navController.navigate(Routes.CONTEXTUAL_COPILOT)
                },
                onUpgradeToPro = {
                    navController.navigate(Routes.CURB_PRO_PAYWALL)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 09. CONTEXTUAL COPILOT
        composable(Routes.CONTEXTUAL_COPILOT) {
            ContextualCopilotScreen(
                messages = chatMessages,
                isLoading = isChatLoading,
                scanResult = currentScanResult,
                usageInfo = chatUsageInfo,
                isPro = isUserPro,
                onSendMessage = { query ->
                    viewModel.sendChatMessage(query)
                },
                onUpgradeToPro = {
                    navController.navigate(Routes.CURB_PRO_PAYWALL)
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 09b. ASK CURB (ALIAS / BACKWARD COMPATIBILITY)
        composable(Routes.ASK_CURB) {
            ContextualCopilotScreen(
                messages = chatMessages,
                isLoading = isChatLoading,
                scanResult = currentScanResult,
                usageInfo = chatUsageInfo,
                isPro = isUserPro,
                onSendMessage = { query ->
                    viewModel.sendChatMessage(query)
                },
                onUpgradeToPro = {
                    navController.navigate(Routes.CURB_PRO_PAYWALL)
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
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
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
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
                onSavedPlacesClicked = { navController.navigate(Routes.SAVED_PLACES) },
                onPaymentSubscriptionClicked = { navController.navigate(Routes.PAYMENT_SUBSCRIPTION) },
                onHelpSupportClicked = { navController.navigate(Routes.HELP_SUPPORT) },
                onPrivacyPolicyClicked = { navController.navigate(Routes.PRIVACY_POLICY) },
                onTermsOfServiceClicked = { navController.navigate(Routes.TERMS_OF_SERVICE) },
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
                    navController.navigate(Routes.PARKING_TIMER) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
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
                isJudgeProActive = isJudgeProActive,
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
                isPro = isUserPro,
                isJudgeProActive = isJudgeProActive,
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
                    // CRITICAL ISSUE 7 FIX: Do not fabricate a scan without a captured image.
                    // Navigate to the real Scan screen so the user can capture the actual parking sign.
                    // The saved place name is available as context but must not replace real sign evidence.
                    navController.navigate(Routes.SCAN)
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
            // Quick-start requires the same authorization the repository enforces: a valid scan
            // result with timer authority and a verified time limit.
            val canQuickStart = ParkingAuthority.canAuthorizeTimer(currentScanResult) &&
                    ParkingTimerCalculator.calculateConfig(currentScanResult).canStart
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
                    navController.navigate(Routes.FIND_MY_CAR) {
                        launchSingleTop = true
                    }
                },
                onStartQuickTimer = { minutes, limitText ->
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val allowedTime = sdf.format(Date(System.currentTimeMillis() + (minutes * 60 * 1000L)))
                    viewModel.startParkingSession(
                        durationMinutes = minutes,
                        allowedUntilTime = allowedTime,
                        scanResult = currentScanResult
                    ) { sessionId ->
                        if (sessionId > 0) {
                            Toast.makeText(context, "Timer started for $minutes minutes", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Couldn't start the parking timer. Please scan a valid parking sign first.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                canStartQuickTimer = canQuickStart,
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
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 22. FIND MY CAR
        composable(Routes.FIND_MY_CAR) {
            val effectiveSpotForFind = if (activeSession?.isDemo == true) demoSavedParkingSpot else savedParkingSpot
            FindMyCarScreen(
                savedParkingSpot = effectiveSpotForFind,
                userLocationState = userLocationState,
                walkingRoute = walkingRoute,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
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
                    navController.navigate(Routes.PARKING_TIMER) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
