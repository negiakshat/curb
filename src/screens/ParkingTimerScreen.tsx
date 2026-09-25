import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  Clock,
  Car,
  Bell,
  Plus,
  Square,
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  Info,
  MapPin,
  X
} from 'lucide-react';
import { ActiveParkingSession, ParkingSpot } from '../types';

interface ParkingTimerScreenProps {
  activeSession: ActiveParkingSession | null;
  savedSpot: ParkingSpot | null;
  onSaveSpot: () => void;
  onEndSession: (id: number) => void;
  onExtendSession: (id: number, minutes: number) => void;
  onUpdateReminder: (id: number, minutes: number) => void;
  onNavigateToFindMyCar: () => void;
  onNavigateToScan: () => void;
  onBack: () => void;
}

export const ParkingTimerScreen: React.FC<ParkingTimerScreenProps> = ({
  activeSession,
  savedSpot,
  onSaveSpot,
  onEndSession,
  onExtendSession,
  onUpdateReminder,
  onNavigateToFindMyCar,
  onNavigateToScan,
  onBack
}) => {
  const [now, setNow] = useState(Date.now());
  const [showExtendModal, setShowExtendModal] = useState(false);
  const [showReminderModal, setShowReminderModal] = useState(false);
  const [showRulesModal, setShowRulesModal] = useState(false);
  const [spotSavedToast, setSpotSavedToast] = useState(false);

  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(interval);
  }, []);

  const hasSession = activeSession && activeSession.isActive;

  const remainingMillis = hasSession ? Math.max(0, activeSession.endTime - now) : 0;
  const isExpired = hasSession && activeSession.endTime <= now;

  const totalDuration = hasSession
    ? Math.max(1000, activeSession.endTime - activeSession.startTime)
    : 1;

  const progressFraction = Math.min(1, Math.max(0, remainingMillis / totalDuration));

  const remainingSecs = Math.floor(remainingMillis / 1000);
  const hours = Math.floor(remainingSecs / 3600);
  const minutes = Math.floor((remainingSecs % 3600) / 60);
  const seconds = remainingSecs % 60;

  const remainingDisplay = isExpired
    ? 'Expired'
    : hours > 0
    ? `${hours}h ${minutes}m`
    : `${minutes}m ${seconds < 10 ? '0' : ''}${seconds}s`;

  const handleSaveSpotClick = () => {
    onSaveSpot();
    setSpotSavedToast(true);
    setTimeout(() => setSpotSavedToast(false), 2500);
  };

  return (
    <div className="min-h-screen bg-[#FDF8F6] pb-24 max-w-md mx-auto">
      {/* Top App Bar */}
      <div className="sticky top-0 z-30 bg-[#FDF8F6]/95 backdrop-blur-md px-5 pt-4 pb-3 flex items-center justify-between border-b border-[#EDE0DC]">
        <button
          onClick={onBack}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center hover:bg-[#F3E9E5] transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>

        <h2 className="text-base font-extrabold text-[#1F1B1A]">
          Parking Timer
        </h2>

        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4">
        {hasSession ? (
          <>
            {/* HERO TIMER DISPLAY */}
            <div className="rounded-3xl bg-[#3A0B01] text-white p-6 shadow-xl relative overflow-hidden text-center space-y-4">
              <div className="flex items-center justify-center">
                <span className="px-3 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-[#FFDAD1] text-[#3A0B01]">
                  {activeSession.timerMode || 'Verified Time Limit'}
                </span>
              </div>

              {/* Countdown Progress Ring */}
              <div className="relative w-44 h-44 mx-auto flex items-center justify-center">
                <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
                  <path
                    className="text-white/10"
                    strokeWidth="3.2"
                    stroke="currentColor"
                    fill="none"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                  <path
                    className={isExpired ? 'text-[#BA1A1A]' : 'text-[#FFDAD1]'}
                    strokeDasharray={`${progressFraction * 100}, 100`}
                    strokeLinecap="round"
                    strokeWidth="3.2"
                    stroke="currentColor"
                    fill="none"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                  />
                </svg>

                <div className="absolute flex flex-col items-center">
                  <span className="text-3xl font-black tracking-tight text-white">
                    {remainingDisplay}
                  </span>
                  <span className="text-[11px] text-[#FFDAD1] font-semibold mt-0.5">
                    {isExpired ? 'Time limit ended' : 'remaining'}
                  </span>
                </div>
              </div>

              <div className="space-y-0.5">
                <p className="text-sm font-bold text-white">
                  {activeSession.locationName}
                </p>
                <p className="text-xs text-[#FFDAD1]/80">
                  Allowed until {activeSession.allowedUntilTime || 'End of limit'}
                </p>
              </div>

              {/* Extend & End Controls */}
              <div className="grid grid-cols-2 gap-3 pt-2">
                <button
                  onClick={() => setShowExtendModal(true)}
                  className="py-3 px-4 rounded-2xl bg-[#8F4C38] text-white text-xs font-bold shadow-md hover:bg-[#A35943] flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Plus className="w-4 h-4" />
                  <span>Extend Time</span>
                </button>

                <button
                  onClick={() => onEndSession(activeSession.id)}
                  className="py-3 px-4 rounded-2xl bg-white/10 hover:bg-white/20 text-white text-xs font-bold border border-white/20 flex items-center justify-center gap-1.5 transition-colors"
                >
                  <Square className="w-3.5 h-3.5 fill-current" />
                  <span>End Session</span>
                </button>
              </div>
            </div>

            {/* SAVE SPOT GPS BUTTON OR PIN STATUS */}
            <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-4 shadow-xs flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center">
                  <Car className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="font-extrabold text-sm text-[#1F1B1A]">
                    {savedSpot ? 'Parking Spot Pinned' : 'Save Spot Location'}
                  </h4>
                  <p className="text-xs text-[#85736E]">
                    {savedSpot
                      ? `${savedSpot.locationName || 'GPS Location saved'}`
                      : 'Remember where you parked for easy return'}
                  </p>
                </div>
              </div>

              {savedSpot ? (
                <button
                  onClick={onNavigateToFindMyCar}
                  className="px-3.5 py-2 rounded-xl bg-[#8F4C38] text-white text-xs font-bold hover:bg-[#3A0B01] shadow-xs"
                >
                  Find Car
                </button>
              ) : (
                <button
                  onClick={handleSaveSpotClick}
                  className="px-3.5 py-2 rounded-xl bg-[#8F4C38] text-white text-xs font-bold hover:bg-[#3A0B01] shadow-xs"
                >
                  {spotSavedToast ? 'Pinned!' : 'Pin Spot'}
                </button>
              )}
            </div>

            {/* SESSION SETTINGS & DETAILS LIST */}
            <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl divide-y divide-[#EDE0DC] shadow-xs overflow-hidden">
              {/* Row 1: Started Time */}
              <div className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Clock className="w-4 h-4 text-[#85736E]" />
                  <div>
                    <h5 className="text-xs font-bold text-[#1F1B1A]">Started at</h5>
                    <p className="text-[11px] text-[#85736E]">
                      {new Date(activeSession.startTime).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} Today
                    </p>
                  </div>
                </div>
                <span className="text-xs font-bold text-[#1F1B1A]">
                  Active
                </span>
              </div>

              {/* Row 2: Reminder Notifications */}
              <div
                onClick={() => setShowReminderModal(true)}
                className="p-4 flex items-center justify-between cursor-pointer hover:bg-[#FDF8F6] transition-colors"
              >
                <div className="flex items-center gap-3">
                  <Bell className="w-4 h-4 text-[#85736E]" />
                  <div>
                    <h5 className="text-xs font-bold text-[#1F1B1A]">Reminder Alert</h5>
                    <p className="text-[11px] text-[#85736E]">
                      {activeSession.reminderMinutesBefore > 0
                        ? `${activeSession.reminderMinutesBefore} minutes before expiry`
                        : 'Disabled'}
                    </p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-[#85736E]" />
              </div>

              {/* Row 3: Active Sign Rules */}
              <div
                onClick={() => setShowRulesModal(true)}
                className="p-4 flex items-center justify-between cursor-pointer hover:bg-[#FDF8F6] transition-colors"
              >
                <div className="flex items-center gap-3">
                  <Info className="w-4 h-4 text-[#85736E]" />
                  <div>
                    <h5 className="text-xs font-bold text-[#1F1B1A]">Active Regulations</h5>
                    <p className="text-[11px] text-[#85736E]">
                      {activeSession.parkingRuleSummary || 'View verified sign rules'}
                    </p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-[#85736E]" />
              </div>
            </div>
          </>
        ) : (
          /* EMPTY STATE (No active session) */
          <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-8 text-center space-y-4 my-8">
            <div className="w-16 h-16 rounded-3xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mx-auto">
              <Clock className="w-8 h-8" />
            </div>

            <div>
              <h3 className="font-black text-lg text-[#1F1B1A]">
                No Active Parking Session
              </h3>
              <p className="text-xs text-[#85736E] mt-1 max-w-xs mx-auto">
                Scan a parking sign with Curb to start an authorized timer verified against posted street rules.
              </p>
            </div>

            <button
              onClick={onNavigateToScan}
              className="py-3.5 px-6 rounded-2xl bg-[#8F4C38] text-white text-xs font-extrabold shadow-md hover:bg-[#3A0B01] transition-all"
            >
              Scan Sign Now
            </button>
          </div>
        )}
      </div>

      {/* EXTEND TIME MODAL */}
      {showExtendModal && activeSession && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-xs p-0 animate-in fade-in duration-200">
          <div className="w-full max-w-md bg-[#FFFFFF] rounded-t-3xl border border-[#EDE0DC] shadow-2xl p-6">
            <div className="flex items-center justify-between pb-3 border-b border-[#EDE0DC]">
              <h3 className="font-black text-lg text-[#1F1B1A]">Extend Parking Session</h3>
              <button
                onClick={() => setShowExtendModal(false)}
                className="w-8 h-8 rounded-full bg-[#F3E9E5] text-[#1F1B1A] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-[#85736E] mt-3">
              Add time to your current session. Make sure extensions comply with municipal limits for this spot.
            </p>

            <div className="grid grid-cols-3 gap-2.5 mt-5">
              {[15, 30, 60].map((mins) => (
                <button
                  key={mins}
                  onClick={() => {
                    onExtendSession(activeSession.id, mins);
                    setShowExtendModal(false);
                  }}
                  className="py-3 rounded-2xl border border-[#EDE0DC] bg-[#FDF8F6] text-xs font-bold text-[#1F1B1A] hover:border-[#8F4C38] hover:bg-[#FFDAD1]/30 transition-all text-center"
                >
                  +{mins >= 60 ? '1 hour' : `${mins} min`}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* REMINDER MODAL */}
      {showReminderModal && activeSession && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-xs p-0 animate-in fade-in duration-200">
          <div className="w-full max-w-md bg-[#FFFFFF] rounded-t-3xl border border-[#EDE0DC] shadow-2xl p-6">
            <div className="flex items-center justify-between pb-3 border-b border-[#EDE0DC]">
              <h3 className="font-black text-lg text-[#1F1B1A]">Reminder Notification</h3>
              <button
                onClick={() => setShowReminderModal(false)}
                className="w-8 h-8 rounded-full bg-[#F3E9E5] text-[#1F1B1A] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2 mt-4">
              {[
                { label: '5 minutes before expiry', value: 5 },
                { label: '10 minutes before expiry', value: 10 },
                { label: '15 minutes before expiry (Recommended)', value: 15 },
                { label: '30 minutes before expiry', value: 30 },
                { label: 'Disable reminders', value: 0 }
              ].map((opt) => (
                <button
                  key={opt.value}
                  onClick={() => {
                    onUpdateReminder(activeSession.id, opt.value);
                    setShowReminderModal(false);
                  }}
                  className={`w-full p-3.5 rounded-2xl border text-xs font-bold flex items-center justify-between text-left transition-all ${
                    activeSession.reminderMinutesBefore === opt.value
                      ? 'border-[#8F4C38] bg-[#FFDAD1]/30 text-[#8F4C38]'
                      : 'border-[#EDE0DC] bg-white text-[#1F1B1A] hover:bg-[#FDF8F6]'
                  }`}
                >
                  <span>{opt.label}</span>
                  {activeSession.reminderMinutesBefore === opt.value && (
                    <CheckCircle2 className="w-4 h-4 text-[#8F4C38]" />
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* RULES MODAL */}
      {showRulesModal && activeSession && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-xs p-0 animate-in fade-in duration-200">
          <div className="w-full max-w-md bg-[#FFFFFF] rounded-t-3xl border border-[#EDE0DC] shadow-2xl p-6 max-h-[70vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-[#EDE0DC]">
              <h3 className="font-black text-lg text-[#1F1B1A]">Active Regulations</h3>
              <button
                onClick={() => setShowRulesModal(false)}
                className="w-8 h-8 rounded-full bg-[#F3E9E5] text-[#1F1B1A] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="mt-4 space-y-3">
              <div className="p-3.5 bg-[#FDF8F6] border border-[#EDE0DC] rounded-2xl">
                <span className="text-[10px] font-bold uppercase tracking-wider text-[#85736E]">
                  Rules Summary
                </span>
                <p className="text-xs font-medium text-[#1F1B1A] mt-1 leading-relaxed">
                  {activeSession.parkingRuleSummary || 'Standard municipal daytime limit.'}
                </p>
              </div>

              {activeSession.notes && (
                <div className="p-3.5 bg-[#FDF8F6] border border-[#EDE0DC] rounded-2xl">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-[#85736E]">
                    Spot Notes
                  </span>
                  <p className="text-xs font-medium text-[#1F1B1A] mt-1">
                    {activeSession.notes}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
