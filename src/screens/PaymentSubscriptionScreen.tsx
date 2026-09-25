import React, { useState } from 'react';
import { ArrowLeft, Sparkles, Check, CreditCard, RefreshCw, Zap } from 'lucide-react';
import { ScanUsageInfo, ChatUsageInfo } from '../types';

interface PaymentSubscriptionScreenProps {
  isPro: boolean;
  scanUsage: ScanUsageInfo;
  chatUsage: ChatUsageInfo;
  onUpgradePro: () => void;
  onApplyPromoCode: (code: string) => boolean;
  onBack: () => void;
}

export const PaymentSubscriptionScreen: React.FC<PaymentSubscriptionScreenProps> = ({
  isPro,
  scanUsage,
  chatUsage,
  onUpgradePro,
  onApplyPromoCode,
  onBack
}) => {
  const [promoCode, setPromoCode] = useState('');
  const [promoMsg, setPromoMsg] = useState<{ text: string; isError: boolean } | null>(null);
  const [isRestoring, setIsRestoring] = useState(false);

  const handleApply = () => {
    if (!promoCode.trim()) return;
    const ok = onApplyPromoCode(promoCode.trim());
    if (ok) {
      setPromoMsg({ text: 'Promo code applied! Curb Pro unlocked.', isError: false });
    } else {
      setPromoMsg({ text: 'Invalid code. Try "CURBPRO2026" or "VIPPARK"', isError: true });
    }
  };

  const handleRestore = () => {
    setIsRestoring(true);
    setTimeout(() => {
      setIsRestoring(false);
      setPromoMsg({ text: 'Purchases up to date.', isError: false });
    }, 1200);
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
          Subscription & Quotas
        </h2>

        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4">
        {/* Current Plan Badge Card */}
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#85736E] uppercase tracking-wider">
              Current Membership
            </span>
            <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold ${
              isPro ? 'bg-[#8F4C38] text-white' : 'bg-[#F3E9E5] text-[#51433F]'
            }`}>
              {isPro ? 'CURB PRO ACTIVE' : 'FREE TIER'}
            </span>
          </div>

          <h3 className="text-2xl font-black text-[#1F1B1A]">
            {isPro ? 'Unlimited Access' : 'Free Account'}
          </h3>

          <p className="text-xs text-[#51433F] leading-relaxed">
            {isPro
              ? 'You have unlimited sign scans, priority Copilot reasoning, and official citation report exports.'
              : 'Free accounts include 10 sign scans per month and 15 Copilot questions per day.'}
          </p>

          {!isPro && (
            <button
              onClick={onUpgradePro}
              className="w-full mt-2 py-3.5 rounded-2xl bg-[#8F4C38] text-white font-extrabold text-xs shadow-md hover:bg-[#3A0B01] flex items-center justify-center gap-2"
            >
              <Sparkles className="w-4 h-4 text-[#FFDAD1]" />
              <span>Upgrade to Curb Pro</span>
            </button>
          )}
        </div>

        {/* Quota Progress */}
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-4">
          <h4 className="text-xs font-bold uppercase tracking-wider text-[#85736E]">
            Usage & Quotas
          </h4>

          {/* Scans Quota */}
          <div>
            <div className="flex justify-between text-xs font-semibold mb-1">
              <span className="text-[#1F1B1A]">Sign Scans (This Month)</span>
              <span className="text-[#8F4C38]">
                {isPro ? 'Unlimited' : `${scanUsage.scansUsedThisMonth} / ${scanUsage.maxFreeScans}`}
              </span>
            </div>
            {!isPro && (
              <div className="w-full h-2 rounded-full bg-[#F3E9E5] overflow-hidden">
                <div
                  className="h-full bg-[#8F4C38] transition-all"
                  style={{
                    width: `${Math.min(100, (scanUsage.scansUsedThisMonth / scanUsage.maxFreeScans) * 100)}%`
                  }}
                />
              </div>
            )}
          </div>

          {/* Copilot Chat Quota */}
          <div>
            <div className="flex justify-between text-xs font-semibold mb-1">
              <span className="text-[#1F1B1A]">Copilot Questions (Today)</span>
              <span className="text-[#8F4C38]">
                {isPro ? 'Unlimited' : `${chatUsage.messagesUsedToday} / ${chatUsage.maxFreeMessages}`}
              </span>
            </div>
            {!isPro && (
              <div className="w-full h-2 rounded-full bg-[#F3E9E5] overflow-hidden">
                <div
                  className="h-full bg-[#8F4C38] transition-all"
                  style={{
                    width: `${Math.min(100, (chatUsage.messagesUsedToday / chatUsage.maxFreeMessages) * 100)}%`
                  }}
                />
              </div>
            )}
          </div>
        </div>

        {/* Promo Code Card */}
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-3">
          <h4 className="text-xs font-bold uppercase tracking-wider text-[#85736E]">
            Redeem Promo Code
          </h4>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="e.g. CURBPRO2026"
              value={promoCode}
              onChange={(e) => setPromoCode(e.target.value)}
              className="flex-1 text-xs uppercase font-bold p-3 rounded-2xl border border-[#D6C2BC] bg-[#FDF8F6] focus:outline-none focus:border-[#8F4C38]"
            />
            <button
              onClick={handleApply}
              className="px-4 py-3 rounded-2xl bg-[#8F4C38] text-white text-xs font-bold hover:bg-[#3A0B01]"
            >
              Redeem
            </button>
          </div>

          {promoMsg && (
            <p className={`text-xs font-semibold ${promoMsg.isError ? 'text-[#BA1A1A]' : 'text-[#2E7D32]'}`}>
              {promoMsg.text}
            </p>
          )}
        </div>

        {/* Restore Purchases */}
        <div className="pt-2">
          <button
            onClick={handleRestore}
            disabled={isRestoring}
            className="w-full py-3 rounded-2xl border border-[#EDE0DC] bg-white text-[#51433F] text-xs font-bold hover:bg-[#F3E9E5] flex items-center justify-center gap-2"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRestoring ? 'animate-spin' : ''}`} />
            <span>Restore In-App Purchases</span>
          </button>
        </div>
      </div>
    </div>
  );
};
