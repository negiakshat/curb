import React, { useState } from 'react';
import {
  ArrowLeft,
  Download,
  Clock,
  MessageSquare,
  Bookmark,
  ChevronRight,
  RotateCcw,
  Check,
  ShieldCheck,
  Info
} from 'lucide-react';
import { ScanResult, CurbNote, SavedPlace } from '../types';
import { ParkingVerdictCard } from '../components/ParkingVerdictCard';
import { NoteSection } from '../components/NoteSection';

interface ScanOutputScreenProps {
  scanResult: ScanResult;
  note: CurbNote | null;
  savedPlaces: SavedPlace[];
  isPro: boolean;
  onSaveNote: (text: string) => void;
  onDeleteNote: () => void;
  onSavePlace: (place: SavedPlace) => void;
  onViewDetails: () => void;
  onStartSession: (durationMins: number, allowedUntil: string, basis: string, rules: string) => void;
  onAskCopilot: () => void;
  onRetake: () => void;
  onUpgradePro: () => void;
  onBack: () => void;
}

export const ScanOutputScreen: React.FC<ScanOutputScreenProps> = ({
  scanResult,
  note,
  savedPlaces,
  isPro,
  onSaveNote,
  onDeleteNote,
  onSavePlace,
  onViewDetails,
  onStartSession,
  onAskCopilot,
  onRetake,
  onUpgradePro,
  onBack
}) => {
  const [spotSaved, setSpotSaved] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);

  const isAllowed = scanResult.verdict === 'ALLOWED';

  // Calculate default session minutes (e.g. 120 mins for 2hr parking)
  const defaultMinutes = (() => {
    if (scanResult.timeRemaining.includes('h')) {
      const match = scanResult.timeRemaining.match(/(\d+)h/);
      if (match) return parseInt(match[1]) * 60;
    }
    return 120;
  })();

  const handleSavePlace = () => {
    onSavePlace({
      id: Date.now(),
      name: scanResult.locationName,
      address: `${scanResult.locationName}, ${scanResult.cityState || 'CA'}`,
      parkingNote: note?.text || scanResult.explanation,
      timestamp: Date.now(),
      latitude: 37.7925,
      longitude: -122.4044,
      scanResultId: scanResult.id,
      parkingRuleSummary: scanResult.parkingRules.join('; '),
      parkingVerdict: scanResult.verdict,
      reminderEnabled: true,
      reminderMinutesBefore: 15
    });
    setSpotSaved(true);
    setTimeout(() => setSpotSaved(false), 2500);
  };

  const handleExport = () => {
    if (!isPro) {
      onUpgradePro();
      return;
    }
    setShowExportModal(true);
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

        <div className="text-center max-w-[200px]">
          <h2 className="text-sm font-extrabold text-[#1F1B1A] truncate">
            {scanResult.locationName}
          </h2>
          <p className="text-[10px] text-[#85736E] font-medium truncate">
            {scanResult.cityState || 'Verified Sign Evidence'}
          </p>
        </div>

        <button
          onClick={handleExport}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center hover:bg-[#F3E9E5] transition-colors"
          title="Export citation defense report"
        >
          <Download className="w-5 h-5 text-[#8F4C38]" />
        </button>
      </div>

      <div className="p-5 space-y-4">
        {/* 1. HERO PARKING DECISION BANNER */}
        <ParkingVerdictCard
          scanResult={scanResult}
          onViewDetails={onViewDetails}
        />

        {/* 2. PRIMARY TIMER CALL TO ACTION (If Parking is Permitted) */}
        {isAllowed && (
          <div className="bg-[#FFFFFF] border-2 border-[#8F4C38] rounded-3xl p-5 shadow-md shadow-[#8F4C38]/10 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-black uppercase tracking-wider text-[#8F4C38] flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-[#8F4C38]" />
                Active Protection
              </span>
              <span className="text-xs font-bold text-[#2E7D32] bg-[#E8F5E9] px-2.5 py-0.5 rounded-full">
                Safe to Park
              </span>
            </div>

            <p className="text-xs font-medium text-[#51433F]">
              Start an authorized timer until <strong>{scanResult.allowedUntilTime}</strong> with customizable 15-minute reminders.
            </p>

            <button
              onClick={() => onStartSession(
                defaultMinutes,
                scanResult.allowedUntilTime,
                'VERIFIED_SIGN_LIMIT',
                scanResult.parkingRules.join('; ')
              )}
              className="w-full py-3.5 rounded-2xl bg-[#8F4C38] text-white text-sm font-extrabold shadow-md hover:bg-[#3A0B01] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
            >
              <Clock className="w-4 h-4" />
              <span>Start Parking Timer</span>
            </button>
          </div>
        )}

        {/* 3. SIGN DETAILS PREVIEW CARD */}
        <div
          onClick={onViewDetails}
          className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-4 shadow-xs flex items-center justify-between cursor-pointer hover:border-[#D6C2BC] transition-all"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center flex-shrink-0">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-extrabold text-sm text-[#1F1B1A]">
                Sign Evidence & Details
              </h4>
              <p className="text-xs text-[#85736E]">
                {scanResult.detectedSigns?.length || 0} physical sign plates analyzed
              </p>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-[#85736E]" />
        </div>

        {/* 4. ASK CURB COPILOT TILE */}
        <div
          onClick={onAskCopilot}
          className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-4 shadow-xs flex items-center justify-between cursor-pointer hover:border-[#D6C2BC] transition-all"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center flex-shrink-0">
              <MessageSquare className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-extrabold text-sm text-[#1F1B1A]">
                Ask Curb Copilot
              </h4>
              <p className="text-xs text-[#85736E]">
                "Can I park after 6 PM?", "Permit exemptions?"
              </p>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-[#85736E]" />
        </div>

        {/* 5. NOTES SECTION */}
        <NoteSection
          note={note}
          onSaveNote={onSaveNote}
          onDeleteNote={onDeleteNote}
          title="Spot Notes"
        />

        {/* 6. BOTTOM ACTIONS: Save Place & Retake */}
        <div className="grid grid-cols-2 gap-3 pt-2">
          <button
            onClick={handleSavePlace}
            className={`py-3 px-4 rounded-2xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all ${
              spotSaved
                ? 'bg-[#E8F5E9] border-[#2E7D32] text-[#2E7D32]'
                : 'bg-white border-[#EDE0DC] text-[#1F1B1A] hover:bg-[#F3E9E5]'
            }`}
          >
            {spotSaved ? (
              <>
                <Check className="w-4 h-4" />
                <span>Spot Saved!</span>
              </>
            ) : (
              <>
                <Bookmark className="w-4 h-4 text-[#8F4C38]" />
                <span>Save Spot</span>
              </>
            )}
          </button>

          <button
            onClick={onRetake}
            className="py-3 px-4 rounded-2xl bg-[#F3E9E5] text-[#1F1B1A] text-xs font-bold hover:bg-[#EDE0DC] flex items-center justify-center gap-1.5 transition-colors"
          >
            <RotateCcw className="w-4 h-4 text-[#8F4C38]" />
            <span>Retake Photo</span>
          </button>
        </div>
      </div>

      {/* EXPORT CITATION MODAL */}
      {showExportModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-sm bg-white rounded-3xl p-6 border border-[#EDE0DC] shadow-xl text-center space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-[#E8F5E9] text-[#2E7D32] flex items-center justify-center mx-auto">
              <Download className="w-6 h-6" />
            </div>
            <div>
              <h3 className="font-extrabold text-lg text-[#1F1B1A]">Parking Evidence Record</h3>
              <p className="text-xs text-[#85736E] mt-1">
                Generated time-stamped proof for {scanResult.locationName} ({scanResult.verdict}).
              </p>
            </div>
            <div className="p-3 bg-[#FDF8F6] rounded-2xl text-left text-xs font-mono text-[#51433F] border border-[#EDE0DC] space-y-1">
              <p>TIMESTAMP: {new Date(scanResult.timestamp).toLocaleString()}</p>
              <p>VERDICT: {scanResult.verdict}</p>
              <p>ALLOWED UNTIL: {scanResult.allowedUntilTime}</p>
              <p>PLATES ANALYZED: {scanResult.detectedSigns?.length || 0}</p>
            </div>
            <button
              onClick={() => setShowExportModal(false)}
              className="w-full py-3 rounded-xl bg-[#8F4C38] text-white text-xs font-bold hover:bg-[#3A0B01]"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
