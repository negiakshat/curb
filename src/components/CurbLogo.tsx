import React from 'react';

interface CurbLogoProps {
  size?: number;
  className?: string;
  showText?: boolean;
}

export const CurbLogo: React.FC<CurbLogoProps> = ({ size = 36, className = '', showText = false }) => {
  return (
    <div className={`flex items-center gap-2.5 ${className}`}>
      <div
        style={{ width: size, height: size }}
        className="rounded-2xl bg-[#8F4C38] flex items-center justify-center shadow-sm text-white font-extrabold select-none flex-shrink-0"
      >
        <span style={{ fontSize: size * 0.58 }} className="leading-none tracking-tight">
          P
        </span>
      </div>
      {showText && (
        <div className="flex flex-col">
          <span className="font-extrabold text-xl tracking-tight text-[#1F1B1A]">Curb</span>
          <span className="text-[10px] uppercase font-bold tracking-wider text-[#8F4C38] -mt-1">Parking Copilot</span>
        </div>
      )}
    </div>
  );
};
