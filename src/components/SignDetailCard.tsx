import React from 'react';
import { AlertTriangle, Check, ShieldAlert } from 'lucide-react';
import { DetectedSign } from '../types';

interface SignDetailCardProps {
  sign: DetectedSign;
  index: number;
}

export const SignDetailCard: React.FC<SignDetailCardProps> = ({ sign, index }) => {
  const isRestricting = sign.isRestrictingNow;
  const isUnclear = sign.isUncertain;

  return (
    <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-2xl p-4 shadow-sm relative overflow-hidden transition-all hover:border-[#D6C2BC]">
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-lg bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center text-xs font-bold">
            #{index + 1}
          </div>
          <h4 className="font-extrabold text-sm text-[#1F1B1A] uppercase tracking-wide">
            {sign.title}
          </h4>
        </div>

        {isRestricting ? (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#FFDAD6] text-[#BA1A1A]">
            <ShieldAlert className="w-3 h-3" />
            Restricting Now
          </span>
        ) : isUnclear ? (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#FFFFDCBE] text-[#9E4800]">
            <AlertTriangle className="w-3 h-3" />
            Unverified
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#E8F5E9] text-[#2E7D32]">
            <Check className="w-3 h-3" />
            Permitted / Inactive
          </span>
        )}
      </div>

      {sign.subtitle && (
        <p className="mt-1 text-xs font-bold text-[#8F4C38] tracking-wide">
          {sign.subtitle}
        </p>
      )}

      {sign.ruleText && (
        <p className="mt-2 text-xs font-medium text-[#51433F] bg-[#FDF8F6] p-2.5 rounded-xl border border-[#EDE0DC]">
          {sign.ruleText}
        </p>
      )}

      {(sign.applicableDaysHours || sign.restrictions) && (
        <div className="mt-2.5 flex flex-wrap gap-2 text-[11px] text-[#85736E]">
          {sign.applicableDaysHours && (
            <span className="bg-[#F3E9E5] px-2 py-0.5 rounded-md font-medium text-[#51433F]">
              📅 {sign.applicableDaysHours}
            </span>
          )}
          {sign.restrictions && (
            <span className="bg-[#F3E9E5] px-2 py-0.5 rounded-md font-medium text-[#51433F]">
              ⚠️ {sign.restrictions}
            </span>
          )}
        </div>
      )}
    </div>
  );
};
