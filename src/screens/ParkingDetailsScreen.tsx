import React, { useState } from 'react';
import {
  ArrowLeft,
  ShieldCheck,
  AlertTriangle,
  Flag,
  CheckCircle2,
  Clock,
  Sparkles
} from 'lucide-react';
import { ScanResult, CurbNote } from '../types';
import { SignDetailCard } from '../components/SignDetailCard';
import { NoteSection } from '../components/NoteSection';

interface ParkingDetailsScreenProps {
  scanResult: ScanResult;
  note: CurbNote | null;
  isPro: boolean;
  onSaveNote: (text: string) => void;
  onDeleteNote: () => void;
  onAskCopilot: () => void;
  onUpgradePro: () => void;
  onBack: () => void;
}

export const ParkingDetailsScreen: React.FC<ParkingDetailsScreenProps> = ({
  scanResult,
  note,
  isPro,
  onSaveNote,
  onDeleteNote,
  onAskCopilot,
  onUpgradePro,
  onBack
}) => {
  const [reportSubmitted, setReportSubmitted] = useState(false);

  const handleReport = () => {
    setReportSubmitted(true);
    setTimeout(() => setReportSubmitted(false), 3000);
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
          Sign Evidence Details
        </h2>

        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4">
        {/* Photo Evidence Card */}
        {scanResult.imageUri && (
          <div className="rounded-3xl overflow-hidden border border-[#EDE0DC] shadow-sm bg-black/5 aspect-16/9 relative">
            <img
              src={scanResult.imageUri}
              alt="Parking sign photo"
              className="w-full h-full object-cover"
            />
            <div className="absolute bottom-2 left-2 bg-black/60 backdrop-blur-md px-2.5 py-1 rounded-xl text-white text-[10px] font-bold">
              Captured Sign Photo
            </div>
          </div>
        )}

        {/* High-Level Overview */}
        <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-5 shadow-xs">
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#85736E]">
            Authority Synthesis
          </span>
          <h3 className="font-extrabold text-base text-[#1F1B1A] mt-1">
            {scanResult.locationName}
          </h3>
          <p className="text-xs text-[#51433F] mt-1 leading-relaxed">
            {scanResult.explanation}
          </p>

          <div className="mt-3 pt-3 border-t border-[#EDE0DC] flex items-center justify-between text-xs">
            <span className="text-[#85736E]">Verdict</span>
            <span className={`px-2 py-0.5 rounded-full font-bold text-[10px] ${
              scanResult.verdict === 'ALLOWED'
                ? 'bg-[#E8F5E9] text-[#2E7D32]'
                : scanResult.verdict === 'RESTRICTED'
                ? 'bg-[#FFDAD6] text-[#BA1A1A]'
                : 'bg-[#FFFFDCBE] text-[#9E4800]'
            }`}>
              {scanResult.verdict}
            </span>
          </div>
        </div>

        {/* Individual Sign Plates List */}
        <div>
          <h3 className="font-black text-sm text-[#1F1B1A] mb-2 px-1">
            Detected Sign Plates ({scanResult.detectedSigns?.length || 0})
          </h3>

          <div className="space-y-3">
            {scanResult.detectedSigns && scanResult.detectedSigns.length > 0 ? (
              scanResult.detectedSigns.map((sign, index) => (
                <SignDetailCard key={sign.id || index} sign={sign} index={index} />
              ))
            ) : (
              <div className="p-4 bg-white border border-[#EDE0DC] rounded-2xl text-center text-xs text-[#85736E]">
                No distinct sign plates cataloged.
              </div>
            )}
          </div>
        </div>

        {/* Notes */}
        <NoteSection
          note={note}
          onSaveNote={onSaveNote}
          onDeleteNote={onDeleteNote}
        />

        {/* Action: Ask Copilot & Report Error */}
        <div className="pt-2 space-y-2.5">
          <button
            onClick={onAskCopilot}
            className="w-full py-3.5 rounded-2xl bg-[#8F4C38] text-white text-xs font-bold shadow-sm hover:bg-[#3A0B01] flex items-center justify-center gap-2"
          >
            <Sparkles className="w-4 h-4 text-[#FFDAD1]" />
            <span>Ask Copilot About This Sign</span>
          </button>

          <button
            onClick={handleReport}
            className="w-full py-3 rounded-2xl border border-[#EDE0DC] bg-white text-[#85736E] text-xs font-bold hover:bg-[#F3E9E5] flex items-center justify-center gap-2"
          >
            <Flag className="w-3.5 h-3.5" />
            <span>{reportSubmitted ? 'Report submitted! Thank you.' : 'Report Inaccurate Sign Reading'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
