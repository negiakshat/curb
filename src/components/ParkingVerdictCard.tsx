import React from 'react';
import { CheckCircle2, AlertOctagon, HelpCircle, Clock, ShieldCheck, DollarSign } from 'lucide-react';
import { ScanResult } from '../types';
import { getVerdictStyles } from '../theme/colors';

interface ParkingVerdictCardProps {
  scanResult: ScanResult;
  className?: string;
  onViewDetails?: () => void;
}

export const ParkingVerdictCard: React.FC<ParkingVerdictCardProps> = ({
  scanResult,
  className = '',
  onViewDetails
}) => {
  const styles = getVerdictStyles(scanResult.verdict);

  const getVerdictIcon = () => {
    switch (scanResult.verdict) {
      case 'ALLOWED':
        return <CheckCircle2 className="w-8 h-8 text-[#2E7D32]" />;
      case 'RESTRICTED':
        return <AlertOctagon className="w-8 h-8 text-[#BA1A1A]" />;
      case 'AMBIGUOUS':
      default:
        return <HelpCircle className="w-8 h-8 text-[#9E4800]" />;
    }
  };

  return (
    <div
      className={`rounded-3xl border ${styles.border} ${styles.bg} p-5 shadow-sm transition-all relative overflow-hidden ${className}`}
    >
      {/* Background tint accent glow */}
      <div className="absolute top-0 right-0 -mt-6 -mr-6 w-32 h-32 rounded-full opacity-20 pointer-events-none blur-xl bg-current" />

      {/* Top row: Verdict Icon & Status Chip */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-2xl bg-white/80 shadow-sm border border-black/5">
            {getVerdictIcon()}
          </div>
          <div>
            <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider ${styles.badge}`}>
              {styles.title}
            </span>
            <h2 className="text-xl font-extrabold text-[#1F1B1A] mt-0.5">
              {scanResult.verdict === 'ALLOWED'
                ? `Park until ${scanResult.allowedUntilTime}`
                : scanResult.verdict === 'RESTRICTED'
                ? 'No parking right now'
                : 'Signage ambiguous'}
            </h2>
          </div>
        </div>

        {scanResult.timeRemaining && scanResult.timeRemaining !== '--' && scanResult.timeRemaining !== '0m' && (
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-2xl bg-white/90 shadow-sm border border-black/5 text-[#1F1B1A]">
            <Clock className="w-4 h-4 text-[#8F4C38]" />
            <span className="text-xs font-bold whitespace-nowrap">{scanResult.timeRemaining}</span>
          </div>
        )}
      </div>

      {/* Explanation Subtitle */}
      <p className="mt-3.5 text-sm font-medium text-[#51433F] leading-relaxed">
        {scanResult.explanation}
      </p>

      {/* Key Badges: Zone & Payment */}
      <div className="mt-4 flex flex-wrap gap-2 pt-3 border-t border-black/5">
        {scanResult.zoneType && (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-white/70 text-xs font-semibold text-[#1F1B1A] border border-black/5">
            <ShieldCheck className="w-3.5 h-3.5 text-[#8F4C38]" />
            <span>{scanResult.zoneType}</span>
          </div>
        )}
        {scanResult.paymentInfo && (
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-white/70 text-xs font-semibold text-[#1F1B1A] border border-black/5">
            <DollarSign className="w-3.5 h-3.5 text-[#2E7D32]" />
            <span>{scanResult.paymentInfo}</span>
          </div>
        )}
      </div>

      {/* Verified Rules List */}
      {scanResult.parkingRules && scanResult.parkingRules.length > 0 && (
        <div className="mt-3.5 space-y-1.5 bg-white/60 rounded-2xl p-3 border border-black/5">
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#85736E]">
            Active Signage Regulations
          </span>
          <ul className="space-y-1 mt-1">
            {scanResult.parkingRules.map((rule, idx) => (
              <li key={idx} className="text-xs font-medium text-[#1F1B1A] flex items-start gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#8F4C38] mt-1.5 flex-shrink-0" />
                <span>{rule}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {onViewDetails && (
        <button
          onClick={onViewDetails}
          className="mt-3 text-xs font-bold text-[#8F4C38] hover:underline flex items-center gap-1 focus:outline-none"
        >
          View all {scanResult.detectedSigns?.length || 0} sign plates & details &rarr;
        </button>
      )}
    </div>
  );
};
