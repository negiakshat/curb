import React, { useState, useEffect } from 'react';
import {
  AppRoute,
  UserProfile,
  ActiveParkingSession,
  ParkingSpot,
  ScanResult,
  SavedPlace,
  CurbNote,
  InAppNotification,
  ChatMessage,
  SampleSignPreset,
  ScanUsageInfo,
  ChatUsageInfo
} from './types';
import { storage } from './services/storage';
import { api } from './services/api';
import { getCurrentUserLocation, UserLocation } from './services/location';

// Components
import { BottomNav } from './components/BottomNav';
import { ProModal } from './components/ProModal';

// Screens
import { SplashScreen } from './screens/SplashScreen';
import { WelcomeScreen } from './screens/WelcomeScreen';
import { NameSetupScreen } from './screens/NameSetupScreen';
import { PermissionsScreen } from './screens/PermissionsScreen';
import { HomeScreen } from './screens/HomeScreen';
import { ScanScreen } from './screens/ScanScreen';
import { ScanOutputScreen } from './screens/ScanOutputScreen';
import { ParkingDetailsScreen } from './screens/ParkingDetailsScreen';
import { ParkingTimerScreen } from './screens/ParkingTimerScreen';
import { FindMyCarScreen } from './screens/FindMyCarScreen';
import { ContextualCopilotScreen } from './screens/ContextualCopilotScreen';
import { ActivityScreen } from './screens/ActivityScreen';
import { SavedPlacesScreen } from './screens/SavedPlacesScreen';
import { YouScreen } from './screens/YouScreen';
import { AccountInfoScreen } from './screens/AccountInfoScreen';
import { NotificationsScreen } from './screens/NotificationsScreen';
import { NotificationSettingsScreen } from './screens/NotificationSettingsScreen';
import { PaymentSubscriptionScreen } from './screens/PaymentSubscriptionScreen';
import { HelpSupportScreen } from './screens/HelpSupportScreen';
import {
  PrivacyPolicyScreen,
  TermsOfServiceScreen,
  AboutCurbScreen
} from './screens/LegalScreens';

export const App: React.FC = () => {
  // Navigation stack state
  const [currentRoute, setCurrentRoute] = useState<AppRoute>('splash');
  const [routeHistory, setRouteHistory] = useState<AppRoute[]>([]);

  // Domain state
  const [userProfile, setUserProfile] = useState<UserProfile>(() => storage.getUserProfile());
  const [activeSession, setActiveSession] = useState<ActiveParkingSession | null>(() => storage.getActiveSession());
  const [savedSpot, setSavedSpot] = useState<ParkingSpot | null>(() => storage.getSavedParkingSpot());
  const [userLocation, setUserLocation] = useState<UserLocation | null>(null);
  const [recentScans, setRecentScans] = useState<ScanResult[]>(() => storage.getScanHistory());
  const [savedPlaces, setSavedPlaces] = useState<SavedPlace[]>(() => storage.getSavedPlaces());
  const [notes, setNotes] = useState<CurbNote[]>(() => storage.getNotes());
  const [notifications, setNotifications] = useState<InAppNotification[]>(() => storage.getNotifications());
  const [scanUsage, setScanUsage] = useState<ScanUsageInfo>(() => storage.getScanUsage());
  const [chatUsage, setChatUsage] = useState<ChatUsageInfo>(() => storage.getChatUsage());

  // Active scan & copilot state
  const [currentScan, setCurrentScan] = useState<ScanResult | null>(() => {
    const history = storage.getScanHistory();
    return history[0] || null;
  });
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [isChatLoading, setIsChatLoading] = useState(false);
  const [isProcessingScan, setIsProcessingScan] = useState(false);
  const [processingStatusText, setProcessingStatusText] = useState('');
  const [scanError, setScanError] = useState<string | null>(null);
  const [presets, setPresets] = useState<SampleSignPreset[]>([]);
  const [isPaywallOpen, setIsPaywallOpen] = useState(false);

  // Initialize presets and location
  useEffect(() => {
    api.getPresets().then((p) => setPresets(p));
    getCurrentUserLocation().then((loc) => setUserLocation(loc));
  }, []);

  // Timer ticker and notification trigger
  useEffect(() => {
    if (!activeSession || !activeSession.isActive) return;

    const checkInterval = setInterval(() => {
      const now = Date.now();
      // Check for expiration
      if (activeSession.endTime <= now) {
        // Session expired
        const ended = { ...activeSession, isActive: false };
        storage.setActiveSession(ended);
        setActiveSession(ended);

        // Add in-app notification
        const notif = storage.addNotification(
          'Parking Session Expired',
          `Your parking time at ${activeSession.locationName} has reached its limit. Check physical street regulations.`,
          activeSession.id
        );
        setNotifications((prev) => [notif, ...prev]);

        // Browser push notification if supported
        if ('Notification' in window && Notification.permission === 'granted' && userProfile.pushNotificationsEnabled) {
          new Notification('Curb Parking Alert', {
            body: `Your parking time at ${activeSession.locationName} has expired!`,
            icon: '/curb_app_icon_1788102263731.jpg'
          });
        }
      }
    }, 1000);

    return () => clearInterval(checkInterval);
  }, [activeSession, userProfile]);

  // Navigation router helpers
  const navigate = (route: AppRoute) => {
    setRouteHistory((prev) => [...prev, currentRoute]);
    setCurrentRoute(route);
    window.scrollTo(0, 0);
  };

  const goBack = () => {
    if (routeHistory.length > 0) {
      const prev = routeHistory[routeHistory.length - 1];
      setRouteHistory((h) => h.slice(0, -1));
      setCurrentRoute(prev);
    } else {
      setCurrentRoute('home');
    }
  };

  // Scan execution handler
  const handleProcessScan = async (imageBase64?: string, presetId?: string) => {
    // Check quota
    if (!userProfile.isPro && scanUsage.scansUsedThisMonth >= scanUsage.maxFreeScans) {
      setIsPaywallOpen(true);
      return;
    }

    setIsProcessingScan(true);
    setScanError(null);
    setProcessingStatusText('Detecting parking sign boundaries & OCR text...');

    try {
      const result = await api.scanSign({
        imageBase64,
        presetId,
        locationName: userLocation?.locationName || 'Current Spot',
        cityState: userLocation?.cityState || 'San Francisco, CA'
      });

      storage.incrementScanUsage();
      setScanUsage(storage.getScanUsage());

      storage.addScanResult(result);
      setRecentScans(storage.getScanHistory());
      setCurrentScan(result);

      setIsProcessingScan(false);
      navigate('scan_output');
    } catch (err: any) {
      setIsProcessingScan(false);
      setScanError(err.message || "Couldn't analyze sign photo. Please try again.");
    }
  };

  // Chat copilot handler
  const handleSendMessage = async (query: string) => {
    // Check quota
    if (!userProfile.isPro && chatUsage.messagesUsedToday >= chatUsage.maxFreeMessages) {
      setIsPaywallOpen(true);
      return;
    }

    const userMsg: ChatMessage = {
      id: `usr_${Date.now()}`,
      text: query,
      isUser: true,
      timestamp: Date.now()
    };

    const newHistory = [...chatMessages, userMsg];
    setChatMessages(newHistory);
    setIsChatLoading(true);

    try {
      const reply = await api.sendChatMessage(query, newHistory, currentScan);
      storage.incrementChatUsage();
      setChatUsage(storage.getChatUsage());

      const botMsg: ChatMessage = {
        id: `bot_${Date.now()}`,
        text: reply,
        isUser: false,
        timestamp: Date.now()
      };
      setChatMessages((prev) => [...prev, botMsg]);
    } catch (err: any) {
      const errorMsg: ChatMessage = {
        id: `err_${Date.now()}`,
        text: "I couldn't reach the parking service. Based on posted signage, please verify active hours on-site.",
        isUser: false,
        timestamp: Date.now()
      };
      setChatMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsChatLoading(false);
    }
  };

  // Start active parking session
  const handleStartParkingSession = (
    durationMins: number,
    allowedUntil: string,
    basis: string,
    rules: string
  ) => {
    const startTime = Date.now();
    const endTime = startTime + durationMins * 60 * 1000;

    const newSession: ActiveParkingSession = {
      id: Date.now(),
      scanResultId: currentScan?.id || Date.now(),
      locationName: currentScan?.locationName || userLocation?.locationName || 'Parked Spot',
      startTime,
      endTime,
      allowedUntilTime: allowedUntil,
      reminderMinutesBefore: 15,
      notes: '',
      timerBasis: basis,
      parkingRuleSummary: rules,
      isActive: true,
      maxAllowedEndTimeMillis: endTime,
      timerMode: 'TIMED_LIMIT'
    };

    storage.setActiveSession(newSession);
    setActiveSession(newSession);

    // Also auto-pin spot if location is available
    if (userLocation) {
      const spot: ParkingSpot = {
        id: Date.now(),
        latitude: userLocation.latitude,
        longitude: userLocation.longitude,
        timestamp: Date.now(),
        accuracy: userLocation.accuracy,
        locationName: currentScan?.locationName || userLocation.locationName,
        sessionId: newSession.id,
        isActive: true
      };
      storage.setSavedParkingSpot(spot);
      setSavedSpot(spot);
    }

    navigate('parking_timer');
  };

  // Pin spot GPS location directly
  const handlePinCurrentLocation = async () => {
    const loc = await getCurrentUserLocation();
    setUserLocation(loc);
    const spot: ParkingSpot = {
      id: Date.now(),
      latitude: loc.latitude,
      longitude: loc.longitude,
      timestamp: Date.now(),
      accuracy: loc.accuracy,
      locationName: loc.locationName,
      sessionId: activeSession?.id || null,
      isActive: true
    };
    storage.setSavedParkingSpot(spot);
    setSavedSpot(spot);
  };

  // Save spot to Saved Places
  const handleSavePlace = (place: SavedPlace) => {
    const saved = storage.saveSavedPlace(place);
    setSavedPlaces(storage.getSavedPlaces());
    return saved;
  };

  // Notes handlers
  const handleSaveNote = (text: string) => {
    if (!currentScan) return;
    storage.saveNote('SCAN_RESULT', currentScan.id, text);
    setNotes(storage.getNotes());
  };

  const handleDeleteNote = () => {
    if (!currentScan) return;
    storage.deleteNote('SCAN_RESULT', currentScan.id);
    setNotes(storage.getNotes());
  };

  // Pro upgrade & Promo code
  const handleUpgradeSuccess = () => {
    const updated = { ...userProfile, isPro: true };
    storage.setUserProfile(updated);
    setUserProfile(updated);
  };

  const handleApplyPromoCode = (code: string) => {
    const clean = code.trim().toUpperCase();
    if (['CURBPRO2026', 'VIPPARK', 'FREEPRO', 'CURBFREE'].includes(clean)) {
      handleUpgradeSuccess();
      return true;
    }
    return false;
  };

  // Delete account & reset
  const handleDeleteAccount = () => {
    storage.clearAllData();
    setUserProfile(storage.getUserProfile());
    setActiveSession(null);
    setSavedSpot(null);
    setRecentScans([]);
    setSavedPlaces([]);
    setNotes([]);
    setNotifications([]);
    setCurrentRoute('welcome');
  };

  const currentScanNote = currentScan
    ? notes.find((n) => n.targetType === 'SCAN_RESULT' && n.targetId === currentScan.id) || null
    : null;

  const hasUnread = notifications.some((n) => !n.isRead);

  // Render current screen
  const renderScreen = () => {
    switch (currentRoute) {
      case 'splash':
        return (
          <SplashScreen
            onFinish={() => {
              if (storage.isOnboardingCompleted()) {
                setCurrentRoute('home');
              } else {
                setCurrentRoute('welcome');
              }
            }}
          />
        );

      case 'welcome':
        return (
          <WelcomeScreen
            onGetStarted={() => navigate('name_setup')}
            onTerms={() => navigate('terms_of_service')}
            onPrivacy={() => navigate('privacy_policy')}
          />
        );

      case 'name_setup':
        return (
          <NameSetupScreen
            currentName={userProfile.name}
            onNameSubmitted={(name) => {
              const updated = { ...userProfile, name };
              storage.setUserProfile(updated);
              setUserProfile(updated);
              navigate('permissions');
            }}
            onBack={goBack}
          />
        );

      case 'permissions':
        return (
          <PermissionsScreen
            onComplete={() => {
              storage.setOnboardingCompleted(true);
              setCurrentRoute('home');
            }}
            onBack={goBack}
          />
        );

      case 'home':
        return (
          <HomeScreen
            userProfile={userProfile}
            activeSession={activeSession}
            savedSpot={savedSpot}
            recentScans={recentScans}
            usageInfo={scanUsage}
            hasUnreadNotifications={hasUnread}
            onNavigate={navigate}
            onSelectScan={(scan) => {
              setCurrentScan(scan);
              navigate('scan_output');
            }}
            onOpenPaywall={() => setIsPaywallOpen(true)}
          />
        );

      case 'scan':
        return (
          <ScanScreen
            isProcessing={isProcessingScan}
            processingStatusText={processingStatusText}
            scanError={scanError}
            usageInfo={scanUsage}
            isPro={userProfile.isPro}
            presets={presets}
            onCaptureImage={(b64) => handleProcessScan(b64)}
            onSelectPreset={(p) => handleProcessScan(undefined, p.id)}
            onClearError={() => setScanError(null)}
            onBack={goBack}
          />
        );

      case 'scan_output':
        return currentScan ? (
          <ScanOutputScreen
            scanResult={currentScan}
            note={currentScanNote}
            savedPlaces={savedPlaces}
            isPro={userProfile.isPro}
            onSaveNote={handleSaveNote}
            onDeleteNote={handleDeleteNote}
            onSavePlace={handleSavePlace}
            onViewDetails={() => navigate('parking_details')}
            onStartSession={handleStartParkingSession}
            onAskCopilot={() => navigate('contextual_copilot')}
            onRetake={() => navigate('scan')}
            onUpgradePro={() => setIsPaywallOpen(true)}
            onBack={goBack}
          />
        ) : (
          <div className="p-6 text-center">
            <p>No scan result available.</p>
            <button onClick={() => navigate('scan')} className="mt-4 px-4 py-2 bg-[#8F4C38] text-white rounded-xl">
              Go to Scan
            </button>
          </div>
        );

      case 'parking_details':
        return currentScan ? (
          <ParkingDetailsScreen
            scanResult={currentScan}
            note={currentScanNote}
            isPro={userProfile.isPro}
            onSaveNote={handleSaveNote}
            onDeleteNote={handleDeleteNote}
            onAskCopilot={() => navigate('contextual_copilot')}
            onUpgradePro={() => setIsPaywallOpen(true)}
            onBack={goBack}
          />
        ) : null;

      case 'parking_timer':
        return (
          <ParkingTimerScreen
            activeSession={activeSession}
            savedSpot={savedSpot}
            onSaveSpot={handlePinCurrentLocation}
            onEndSession={(id) => {
              if (activeSession && activeSession.id === id) {
                const ended = { ...activeSession, isActive: false };
                storage.setActiveSession(ended);
                setActiveSession(ended);
              }
            }}
            onExtendSession={(id, mins) => {
              if (activeSession && activeSession.id === id) {
                const extended = {
                  ...activeSession,
                  endTime: activeSession.endTime + mins * 60 * 1000
                };
                storage.setActiveSession(extended);
                setActiveSession(extended);
              }
            }}
            onUpdateReminder={(id, mins) => {
              if (activeSession && activeSession.id === id) {
                const updated = { ...activeSession, reminderMinutesBefore: mins };
                storage.setActiveSession(updated);
                setActiveSession(updated);
              }
            }}
            onNavigateToFindMyCar={() => navigate('find_my_car')}
            onNavigateToScan={() => navigate('scan')}
            onBack={goBack}
          />
        );

      case 'find_my_car':
        return (
          <FindMyCarScreen
            savedSpot={savedSpot}
            userLoc={userLocation}
            onRefreshLocation={async () => {
              const loc = await getCurrentUserLocation();
              setUserLocation(loc);
            }}
            onSaveCurrentLocation={handlePinCurrentLocation}
            onClearSpot={() => {
              storage.setSavedParkingSpot(null);
              setSavedSpot(null);
            }}
            onNavigateToTimer={() => navigate('parking_timer')}
            onBack={goBack}
          />
        );

      case 'contextual_copilot':
      case 'ask_curb':
        return (
          <ContextualCopilotScreen
            messages={chatMessages}
            isLoading={isChatLoading}
            scanResult={currentScan}
            usageInfo={chatUsage}
            isPro={userProfile.isPro}
            onSendMessage={handleSendMessage}
            onUpgradePro={() => setIsPaywallOpen(true)}
            onBack={goBack}
          />
        );

      case 'activity':
        return (
          <ActivityScreen
            scans={recentScans}
            isPro={userProfile.isPro}
            onSelectScan={(scan) => {
              setCurrentScan(scan);
              navigate('scan_output');
            }}
            onUpgradePro={() => setIsPaywallOpen(true)}
            onBack={goBack}
          />
        );

      case 'saved_places':
        return (
          <SavedPlacesScreen
            savedPlaces={savedPlaces}
            isPro={userProfile.isPro}
            onDeletePlace={(id) => {
              storage.deleteSavedPlace(id);
              setSavedPlaces(storage.getSavedPlaces());
            }}
            onCheckSignAgain={(place) => {
              handleProcessScan(undefined, 'preset_allowed');
            }}
            onOpenScan={() => navigate('scan')}
            onUpgradePro={() => setIsPaywallOpen(true)}
            onBack={goBack}
          />
        );

      case 'you':
        return (
          <YouScreen
            userProfile={userProfile}
            isPro={userProfile.isPro}
            onNavigate={navigate}
            onLogout={handleDeleteAccount}
            onOpenPaywall={() => setIsPaywallOpen(true)}
          />
        );

      case 'account_info':
        return (
          <AccountInfoScreen
            userProfile={userProfile}
            onSaveProfile={(name, gender, email) => {
              const updated = { ...userProfile, name, gender, email };
              storage.setUserProfile(updated);
              setUserProfile(updated);
            }}
            onDeleteAccount={handleDeleteAccount}
            onBack={goBack}
          />
        );

      case 'notifications':
        return (
          <NotificationsScreen
            notifications={notifications}
            onNotificationClick={(sessionId) => {
              navigate('parking_timer');
            }}
            onOpenSettings={() => navigate('notification_settings')}
            onMarkAllRead={() => {
              storage.markNotificationsAsRead();
              setNotifications(storage.getNotifications());
            }}
            onBack={goBack}
          />
        );

      case 'notification_settings':
        return (
          <NotificationSettingsScreen
            pushEnabled={userProfile.pushNotificationsEnabled}
            onTogglePush={(enabled) => {
              const updated = { ...userProfile, pushNotificationsEnabled: enabled };
              storage.setUserProfile(updated);
              setUserProfile(updated);
            }}
            onBack={goBack}
          />
        );

      case 'payment_subscription':
      case 'curb_pro_paywall':
        return (
          <PaymentSubscriptionScreen
            isPro={userProfile.isPro}
            scanUsage={scanUsage}
            chatUsage={chatUsage}
            onUpgradePro={() => setIsPaywallOpen(true)}
            onApplyPromoCode={handleApplyPromoCode}
            onBack={goBack}
          />
        );

      case 'help_support':
        return (
          <HelpSupportScreen
            onTerms={() => navigate('terms_of_service')}
            onPrivacy={() => navigate('privacy_policy')}
            onBack={goBack}
          />
        );

      case 'privacy_policy':
        return <PrivacyPolicyScreen onBack={goBack} />;

      case 'terms_of_service':
        return <TermsOfServiceScreen onBack={goBack} />;

      case 'about_curb':
        return <AboutCurbScreen onBack={goBack} />;

      default:
        return <HomeScreen
          userProfile={userProfile}
          activeSession={activeSession}
          savedSpot={savedSpot}
          recentScans={recentScans}
          usageInfo={scanUsage}
          hasUnreadNotifications={hasUnread}
          onNavigate={navigate}
          onSelectScan={(s) => {
            setCurrentScan(s);
            navigate('scan_output');
          }}
          onOpenPaywall={() => setIsPaywallOpen(true)}
        />;
    }
  };

  // Show Bottom Navigation only on main tabs
  const showBottomNav = ['home', 'activity', 'you'].includes(currentRoute);

  return (
    <div className="min-h-screen bg-[#FDF8F6] text-[#1F1B1A]">
      {renderScreen()}

      {showBottomNav && (
        <BottomNav
          currentRoute={currentRoute}
          onNavigate={navigate}
          hasActiveSession={activeSession?.isActive}
        />
      )}

      {/* Curb Pro Paywall Modal */}
      <ProModal
        isOpen={isPaywallOpen}
        onClose={() => setIsPaywallOpen(false)}
        onUpgradeSuccess={handleUpgradeSuccess}
      />
    </div>
  );
};
