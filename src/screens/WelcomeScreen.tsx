import React from 'react';
import { ArrowRight, ShieldCheck, Camera, BellRing } from 'lucide-react';
import { CurbLogo } from '../components/CurbLogo';

interface WelcomeScreenProps {
  onGetStarted: () => void;
  onTerms: () => void;
  onPrivacy: () => void;
}

export const WelcomeScreen: React.FC<WelcomeScreenProps> = ({
  onGetStarted,
  onTerms,
  onPrivacy
}) => {
  return (
    <div className="min-h-screen bg-[#FDF8F6] flex flex-col justify-between p-6 max-w-md mx-auto">
      {/* Top Bar with Brand */}
      <div className="pt-4 flex items-center justify-between">
        <CurbLogo size={42} showText />
        <span className="text-[11px] font-bold px-2.5 py-1 rounded-full bg-[#FFDAD1] text-[#8F4C38]">
          v1.0
        </span>
      </div>

      {/* Hero Visual Area */}
      <div className="my-auto py-8">
        <div className="relative rounded-3xl overflow-hidden border border-[#EDE0DC] shadow-md aspect-4/3 bg-[#F3E9E5]">
          <img
            src="/welcome_city_1788102283275.jpg"
            alt="City Parking Assistant"
            className="w-full h-full object-cover"
            onError={(e) => {
              // fallback image if path is unavailable
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent flex flex-col justify-end p-5 text-white">
            <span className="text-[11px] font-extrabold uppercase tracking-wider text-[#FFDAD1]">
              Instant Sign Interpretation
            </span>
            <p className="text-sm font-bold mt-1 text-white/95">
              Decodes 2-hour limits, street sweepers, and tow-away corridors in seconds.
            </p>
          </div>
        </div>

        <div className="mt-8 space-y-2">
          <h1 className="text-3xl font-black text-[#1F1B1A] tracking-tight leading-tight">
            Never second-guess parking signs again.
          </h1>
          <p className="text-sm text-[#51433F] leading-relaxed">
            Snap a picture of confusing street signage. Curb highlights restrictions, calculates countdown timers, and remembers where you parked.
          </p>
        </div>

        {/* 3 Key Feature Pills */}
        <div className="mt-5 grid grid-cols-3 gap-2">
          <div className="bg-[#FFFFFF] p-2.5 rounded-2xl border border-[#EDE0DC] text-center">
            <Camera className="w-4 h-4 text-[#8F4C38] mx-auto mb-1" />
            <span className="text-[10px] font-bold text-[#1F1B1A] block">Camera Scan</span>
          </div>
          <div className="bg-[#FFFFFF] p-2.5 rounded-2xl border border-[#EDE0DC] text-center">
            <ShieldCheck className="w-4 h-4 text-[#2E7D32] mx-auto mb-1" />
            <span className="text-[10px] font-bold text-[#1F1B1A] block">Instant Verdict</span>
          </div>
          <div className="bg-[#FFFFFF] p-2.5 rounded-2xl border border-[#EDE0DC] text-center">
            <BellRing className="w-4 h-4 text-[#8F4C38] mx-auto mb-1" />
            <span className="text-[10px] font-bold text-[#1F1B1A] block">Expiry Timers</span>
          </div>
        </div>
      </div>

      {/* Bottom CTA & Legal */}
      <div className="pt-4 space-y-4">
        <button
          onClick={onGetStarted}
          className="w-full py-4 rounded-2xl bg-[#8F4C38] text-white font-extrabold text-base shadow-lg shadow-[#8F4C38]/25 hover:bg-[#3A0B01] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
        >
          <span>Get Started</span>
          <ArrowRight className="w-5 h-5" />
        </button>

        <p className="text-[11px] text-[#85736E] text-center leading-normal">
          By continuing, you agree to Curb's{' '}
          <button onClick={onTerms} className="underline text-[#8F4C38] font-semibold">
            Terms of Service
          </button>{' '}
          and{' '}
          <button onClick={onPrivacy} className="underline text-[#8F4C38] font-semibold">
            Privacy Policy
          </button>.
        </p>
      </div>
    </div>
  );
};
