import React from 'react';
import { ArrowLeft, Shield, FileText, Info } from 'lucide-react';
import { CurbLogo } from '../components/CurbLogo';

interface LegalScreenProps {
  onBack: () => void;
}

export const PrivacyPolicyScreen: React.FC<LegalScreenProps> = ({ onBack }) => {
  return (
    <div className="min-h-screen bg-[#FDF8F6] pb-24 max-w-md mx-auto">
      <div className="sticky top-0 z-30 bg-[#FDF8F6]/95 backdrop-blur-md px-5 pt-4 pb-3 flex items-center justify-between border-b border-[#EDE0DC]">
        <button
          onClick={onBack}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center hover:bg-[#F3E9E5]"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-extrabold text-[#1F1B1A]">Privacy Policy</h2>
        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4 text-xs font-medium text-[#51433F] leading-relaxed">
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-3">
          <h3 className="font-extrabold text-sm text-[#1F1B1A]">Your Privacy Matters</h3>
          <p>
            Curb is built with privacy-by-design principles. Sign photos captured by your device are analyzed solely to resolve parking regulations and extract active municipal schedules.
          </p>
          <h4 className="font-bold text-xs text-[#1F1B1A] pt-2">Data We Collect</h4>
          <p>
            • Sign photos and OCR text for active parking analysis.<br/>
            • Device GPS coordinates strictly to pin your parking spot and resolve city parking code.<br/>
            • App preferences stored locally on your device.
          </p>
          <h4 className="font-bold text-xs text-[#1F1B1A] pt-2">Data Retention</h4>
          <p>
            Scans and notes are stored locally in your browser storage. You can delete individual scans or clear all app data at any time in Account Info.
          </p>
        </div>
      </div>
    </div>
  );
};

export const TermsOfServiceScreen: React.FC<LegalScreenProps> = ({ onBack }) => {
  return (
    <div className="min-h-screen bg-[#FDF8F6] pb-24 max-w-md mx-auto">
      <div className="sticky top-0 z-30 bg-[#FDF8F6]/95 backdrop-blur-md px-5 pt-4 pb-3 flex items-center justify-between border-b border-[#EDE0DC]">
        <button
          onClick={onBack}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center hover:bg-[#F3E9E5]"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-extrabold text-[#1F1B1A]">Terms of Service</h2>
        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4 text-xs font-medium text-[#51433F] leading-relaxed">
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-3">
          <h3 className="font-extrabold text-sm text-[#1F1B1A]">Terms of Use</h3>
          <p>
            Welcome to Curb. By using our application, you agree to these terms. Curb provides parking sign interpretation, rule synthesis, and timer assistance.
          </p>
          <h4 className="font-bold text-xs text-[#1F1B1A] pt-2">Driver Responsibility</h4>
          <p>
            Curb acts as an advisory tool. While our computer vision and AI models provide high-accuracy interpretations, official municipal postings and on-site signage always govern parking legality. Always follow posted signs, emergency notices, and lawful officer directions.
          </p>
          <h4 className="font-bold text-xs text-[#1F1B1A] pt-2">Subscriptions & Pro</h4>
          <p>
            Curb Pro subscriptions provide unlimited scans, continuous timer notifications, and citation defense evidence records. Subscriptions renew automatically unless cancelled prior to the billing cycle.
          </p>
        </div>
      </div>
    </div>
  );
};

export const AboutCurbScreen: React.FC<LegalScreenProps> = ({ onBack }) => {
  return (
    <div className="min-h-screen bg-[#FDF8F6] pb-24 max-w-md mx-auto">
      <div className="sticky top-0 z-30 bg-[#FDF8F6]/95 backdrop-blur-md px-5 pt-4 pb-3 flex items-center justify-between border-b border-[#EDE0DC]">
        <button
          onClick={onBack}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center hover:bg-[#F3E9E5]"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-extrabold text-[#1F1B1A]">About Curb</h2>
        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4 text-center">
        <div className="py-6 space-y-3">
          <CurbLogo size={56} className="justify-center" />
          <h3 className="font-black text-xl text-[#1F1B1A]">Curb</h3>
          <p className="text-xs uppercase font-extrabold tracking-widest text-[#8F4C38]">
            Version 1.0.0
          </p>
          <p className="text-xs text-[#51433F] max-w-xs mx-auto leading-relaxed pt-2">
            The intelligent municipal parking assistant that eliminates parking ticket anxiety through vision AI, live countdown timers, and contextual sign reasoning.
          </p>
        </div>

        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs text-left space-y-3 text-xs text-[#51433F]">
          <h4 className="font-extrabold text-xs text-[#1F1B1A] uppercase tracking-wider text-[#85736E]">
            Key Technology
          </h4>
          <p>• Multi-sign visual synthesis & bounding box detection</p>
          <p>• Temporal rule resolver & street sweeping schedule parser</p>
          <p>• Precision spot pinning & reverse geocoded coordinates</p>
          <p>• Powered by Google Gemini reasoning</p>
        </div>
      </div>
    </div>
  );
};
