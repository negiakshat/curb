import React, { useEffect } from 'react';
import { CurbLogo } from '../components/CurbLogo';

interface SplashScreenProps {
  onFinish: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({ onFinish }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onFinish();
    }, 1200);
    return () => clearTimeout(timer);
  }, [onFinish]);

  return (
    <div className="min-h-screen bg-[#FDF8F6] flex flex-col items-center justify-center p-6 select-none animate-in fade-in duration-300">
      <div className="flex flex-col items-center gap-4">
        <div className="w-20 h-20 rounded-3xl bg-[#8F4C38] text-white flex items-center justify-center shadow-xl shadow-[#8F4C38]/20 animate-pulse-slow">
          <span className="text-4xl font-extrabold tracking-tight">P</span>
        </div>
        <div className="text-center">
          <h1 className="text-3xl font-black tracking-tight text-[#1F1B1A]">Curb</h1>
          <p className="text-xs uppercase tracking-widest font-bold text-[#8F4C38] mt-1">
            Parking Copilot & Vision AI
          </p>
        </div>
      </div>

      <div className="absolute bottom-10 flex flex-col items-center gap-2">
        <div className="w-6 h-6 border-2 border-[#8F4C38]/30 border-t-[#8F4C38] rounded-full animate-spin" />
        <span className="text-[11px] font-semibold text-[#85736E]">Loading parking authority data...</span>
      </div>
    </div>
  );
};
