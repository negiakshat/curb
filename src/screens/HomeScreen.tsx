import React, { useState, useEffect } from 'react';
import {
  Bell,
  Camera,
  Clock,
  Car,
  Bookmark,
  MessageSquare,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  Navigation
} from 'lucide-react';
import {
  UserProfile,
  ActiveParkingSession,
  ParkingSpot,
  ScanResult,
  ScanUsageInfo,
  AppRoute
} from '../types';
import { CurbLogo } from '../components/CurbLogo';
import { getVerdictStyles } from '../theme/colors';

interface HomeScreenProps {
  userProfile: UserProfile;
  activeSession: ActiveParkingSession | null;
  savedSpot: ParkingSpot | null;
  recentScans: ScanResult[];
  usageInfo: ScanUsageInfo;
  hasUnreadNotifications: boolean;
  onNavigate: (route: AppRoute) => void;
  onSelectScan: (scan: ScanResult) => void;
  onOpenPaywall: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  userProfile,
  activeSession,
  savedSpot,
  recentScans,
  usageInfo,
  hasUnreadNotifications,
  onNavigate,
  onSelectScan,
  onOpenPaywall
}) => {
  // Live timer state
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    if (activeSession && activeSession.isActive) {
      const interval = setInterval(() => setNow(Date.now()), 1000);
      return () => clearInterval(interval);
    }
  }, [activeSession]);

  const greeting = (() => {
    const hour = new Date().getHours();
    if (hour >= 4 && hour < 12) return 'Good morning';
    if (hour >= 12 && hour < 18) return 'Good afternoon';
    return 'Good evening';
  })();

  const dateString = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'short',
    day: 'numeric'
  });

  // Calculate remaining timer
  const remainingMillis = activeSession && activeSession.isActive
    ? Math.max(0, activeSession.endTime - now)
    : 0;

  const totalMillis = activeSession && activeSession.isActive
    ? Math.max(1000, activeSession.endTime - activeSession.startTime)
    : 1;

  const progressFraction = Math.min(1, Math.max(0, remainingMillis / totalMillis));
  const remainingSecs = Math.floor(remainingMillis / 1000);
  const remHours = Math.floor(remainingSecs / 3600);
  const remMinutes = Math.floor((remainingSecs % 3600) / 60);
  const remSeconds = remainingSecs % 60;

  const remainingFormatted = remHours > 0
    ? `${remHours}h ${remMinutes}m`
    : `${remMinutes}m ${remSeconds < 10 ? '0' : ''}${remSeconds}s`;

  return (
    <div className="min-h-screen bg-[#FDF8F6] pb-24 max-w-md mx-auto">
      {/* Top App Bar */}
      <div className="sticky top-0 z-30 bg-[#FDF8F6]/90 backdrop-blur-md px-5 pt-4 pb-3 flex items-center justify-between border-b border-[#EDE0DC]">
        <div className="flex items-center gap-3">
          <CurbLogo size={38} />
          <div>
            <h1 className="text-base font-extrabold text-[#1F1B1A] leading-tight">
              {greeting}, {userProfile.name}
            </h1>
            <p className="text-[11px] font-semibold text-[#85736E]">{dateString}</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Notification Center button */}
          <button
            onClick={() => onNavigate('notifications')}
            className="w-10 h-10 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center relative hover:bg-[#F3E9E5] transition-colors"
            title="Notifications"
          >
            <Bell className="w-5 h-5 text-[#51433F]" />
            {hasUnreadNotifications && (
              <span className="absolute top-2 right-2 w-2.5 h-2.5 rounded-full bg-[#BA1A1A] ring-2 ring-white" />
            )}
          </button>

          {/* Profile Avatar button */}
          <button
            onClick={() => onNavigate('you')}
            className="w-10 h-10 rounded-full bg-[#FFDAD1] border-2 border-[#8F4C38] text-[#8F4C38] font-bold text-sm flex items-center justify-center hover:opacity-90 transition-opacity"
            title="Your Profile"
          >
            {userProfile.name.charAt(0).toUpperCase()}
          </button>
        </div>
      </div>

      <div className="px-5 pt-4 space-y-4">
        {/* ACTIVE SESSION TIMER CARD (If Active) */}
        {activeSession && activeSession.isActive && (
          <div
            onClick={() => onNavigate('parking_timer')}
            className="rounded-3xl bg-[#3A0B01] text-white p-5 shadow-lg shadow-[#3A0B01]/20 cursor-pointer relative overflow-hidden transition-all hover:scale-[1.01]"
          >
            <div className="flex items-start justify-between">
              <div>
                <span className="inline-block px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-[#FFDAD1] text-[#3A0B01]">
                  Live Parking Session
                </span>
                <h3 className="text-2xl font-black mt-2 tracking-tight">
                  {remainingFormatted}
                </h3>
                <p className="text-xs text-[#FFDAD1]/80 mt-0.5">
                  Allowed until {activeSession.allowedUntilTime || 'End of limit'} • {activeSession.locationName}
                </p>
              </div>

              {/* Circular Progress Ring */}
              <div className="relative w-14 h-14 flex items-center justify-center flex-shrink-0">
                <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
                  <path
                    className="text-white/20"
                    strokeWidth="3.5"
                    stroke="currentColor"
                    fill="none"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                  <path
                    className="text-[#FFDAD1]"
                    strokeDasharray={`${progressFraction * 100}, 100`}
                    strokeLinecap="round"
                    strokeWidth="3.5"
                    stroke="currentColor"
                    fill="none"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                </svg>
                <Clock className="w-5 h-5 text-[#FFDAD1] absolute" />
              </div>
            </div>

            <div className="mt-4 pt-3 border-t border-white/10 flex items-center justify-between text-xs text-[#FFDAD1]">
              <span>Tap to manage, extend, or view rules</span>
              <ArrowRight className="w-4 h-4" />
            </div>
          </div>
        )}

        {/* HERO SCAN ACTION CARD */}
        <div
          onClick={() => onNavigate('scan')}
          className="rounded-3xl bg-[#8F4C38] text-white p-5 shadow-lg shadow-[#8F4C38]/20 cursor-pointer relative overflow-hidden group transition-all active:scale-[0.99]"
        >
          <div className="absolute top-0 right-0 -mr-6 -mt-6 w-36 h-36 rounded-full bg-white/10 pointer-events-none blur-xl group-hover:scale-110 transition-transform" />
          
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-white/15 backdrop-blur-xs flex items-center justify-center border border-white/20 group-hover:scale-105 transition-transform flex-shrink-0">
              <Camera className="w-7 h-7 text-white stroke-[2.2]" />
            </div>
            <div>
              <span className="inline-block px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-[#FFDAD1] text-[#8F4C38]">
                Instant Vision AI
              </span>
              <h2 className="text-xl font-black mt-1 text-white tracking-tight">
                Scan Parking Sign
              </h2>
              <p className="text-xs text-white/80 mt-0.5">
                Point camera or select photo to check if parking is allowed right now
              </p>
            </div>
          </div>
        </div>

        {/* BENTO GRID: 4 KEY TILES */}
        <div className="grid grid-cols-2 gap-3">
          {/* Tile 1: Parking Timer */}
          <div
            onClick={() => onNavigate('parking_timer')}
            className="p-4 rounded-3xl bg-[#FFFFFF] border border-[#EDE0DC] shadow-xs cursor-pointer hover:border-[#D6C2BC] transition-all"
          >
            <div className="w-10 h-10 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mb-3">
              <Clock className="w-5 h-5" />
            </div>
            <h3 className="font-extrabold text-sm text-[#1F1B1A]">Parking Timer</h3>
            <p className="text-[11px] text-[#85736E] mt-0.5">
              {activeSession?.isActive ? 'Active session running' : 'Start countdown'}
            </p>
          </div>

          {/* Tile 2: Find My Car */}
          <div
            onClick={() => onNavigate('find_my_car')}
            className="p-4 rounded-3xl bg-[#FFFFFF] border border-[#EDE0DC] shadow-xs cursor-pointer hover:border-[#D6C2BC] transition-all"
          >
            <div className="w-10 h-10 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mb-3">
              <Car className="w-5 h-5" />
            </div>
            <h3 className="font-extrabold text-sm text-[#1F1B1A]">Find My Car</h3>
            <p className="text-[11px] text-[#85736E] mt-0.5 truncate">
              {savedSpot ? (savedSpot.locationName || 'Spot pinned') : 'Pin your spot'}
            </p>
          </div>

          {/* Tile 3: Saved Places */}
          <div
            onClick={() => onNavigate('saved_places')}
            className="p-4 rounded-3xl bg-[#FFFFFF] border border-[#EDE0DC] shadow-xs cursor-pointer hover:border-[#D6C2BC] transition-all"
          >
            <div className="w-10 h-10 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mb-3">
              <Bookmark className="w-5 h-5" />
            </div>
            <h3 className="font-extrabold text-sm text-[#1F1B1A]">Saved Places</h3>
            <p className="text-[11px] text-[#85736E] mt-0.5">
              Favorite spots & rules
            </p>
          </div>

          {/* Tile 4: Ask Curb Copilot */}
          <div
            onClick={() => onNavigate('ask_curb')}
            className="p-4 rounded-3xl bg-[#FFFFFF] border border-[#EDE0DC] shadow-xs cursor-pointer hover:border-[#D6C2BC] transition-all"
          >
            <div className="w-10 h-10 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mb-3">
              <MessageSquare className="w-5 h-5" />
            </div>
            <h3 className="font-extrabold text-sm text-[#1F1B1A]">Ask Curb</h3>
            <p className="text-[11px] text-[#85736E] mt-0.5">
              Parking law Q&A copilot
            </p>
          </div>
        </div>

        {/* PRO QUOTA / UPGRADE BANNER (If not pro) */}
        {!userProfile.isPro && (
          <div
            onClick={onOpenPaywall}
            className="p-4 rounded-3xl bg-[#FFDAD1]/50 border border-[#EDE0DC] flex items-center justify-between cursor-pointer hover:bg-[#FFDAD1]/70 transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-2xl bg-[#8F4C38] text-white flex items-center justify-center flex-shrink-0">
                <Sparkles className="w-4 h-4" />
              </div>
              <div>
                <h4 className="text-xs font-black text-[#1F1B1A]">
                  Free Scans: {Math.max(0, usageInfo.maxFreeScans - usageInfo.scansUsedThisMonth)} remaining
                </h4>
                <p className="text-[11px] text-[#51433F]">
                  Upgrade to Curb Pro for unlimited scans & citation defense.
                </p>
              </div>
            </div>
            <ArrowRight className="w-4 h-4 text-[#8F4C38] flex-shrink-0" />
          </div>
        )}

        {/* RECENT SCANS SECTION */}
        <div className="pt-2">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-black text-base text-[#1F1B1A]">Recent Scans</h3>
            {recentScans.length > 0 && (
              <button
                onClick={() => onNavigate('activity')}
                className="text-xs font-bold text-[#8F4C38] hover:underline"
              >
                View all
              </button>
            )}
          </div>

          {recentScans.length === 0 ? (
            <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-6 text-center">
              <div className="w-12 h-12 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mx-auto mb-3">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h4 className="font-extrabold text-sm text-[#1F1B1A]">No scans yet</h4>
              <p className="text-xs text-[#85736E] mt-1 max-w-xs mx-auto">
                Scan your first parking sign or test a preset to see rules broken down into a clear answer.
              </p>
              <button
                onClick={() => onNavigate('scan')}
                className="mt-4 px-4 py-2 rounded-xl bg-[#8F4C38] text-white text-xs font-bold shadow-xs hover:bg-[#3A0B01]"
              >
                Scan Now
              </button>
            </div>
          ) : (
            <div className="space-y-2.5">
              {recentScans.slice(0, 3).map((scan) => {
                const styles = getVerdictStyles(scan.verdict);
                return (
                  <div
                    key={scan.id}
                    onClick={() => onSelectScan(scan)}
                    className="p-3.5 rounded-2xl bg-[#FFFFFF] border border-[#EDE0DC] shadow-xs flex items-center justify-between cursor-pointer hover:border-[#D6C2BC] transition-all"
                  >
                    <div className="flex items-center gap-3">
                      <div className={`w-8 h-8 rounded-xl ${styles.bg} ${styles.text} flex items-center justify-center text-xs font-black`}>
                        {scan.verdict === 'ALLOWED' ? '✓' : scan.verdict === 'RESTRICTED' ? '✕' : '?'}
                      </div>
                      <div>
                        <h4 className="font-extrabold text-xs text-[#1F1B1A]">
                          {scan.locationName}
                        </h4>
                        <p className="text-[10px] text-[#85736E]">
                          {new Date(scan.timestamp).toLocaleDateString()} • {scan.allowedUntilTime}
                        </p>
                      </div>
                    </div>

                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${styles.badge}`}>
                      {scan.verdict}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
