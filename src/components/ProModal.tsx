import React, { useState } from 'react';
import { X, Sparkles, Check, Shield, Zap, Bell, FileText, ArrowRight } from 'lucide-react';
import confetti from 'canvas-confetti';

interface ProModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpgradeSuccess: () => void;
}

export const ProModal: React.FC<ProModalProps> = ({ isOpen, onClose, onUpgradeSuccess }) => {
  const [selectedPlan, setSelectedPlan] = useState<'annual' | 'monthly'>('annual');
  const [promoCode, setPromoCode] = useState('');
  const [promoError, setPromoError] = useState('');
  const [promoSuccess, setPromoSuccess] = useState(false);

  if (!isOpen) return null;

  const triggerConfetti = () => {
    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 }
    });
  };

  const handlePurchase = () => {
    triggerConfetti();
    onUpgradeSuccess();
    onClose();
  };

  const handleApplyPromo = () => {
    const code = promoCode.trim().toUpperCase();
    if (['CURBPRO2026', 'VIPPARK', 'FREEPRO', 'CURBFREE'].includes(code)) {
      setPromoSuccess(true);
      setPromoError('');
      triggerConfetti();
      setTimeout(() => {
        onUpgradeSuccess();
        onClose();
      }, 800);
    } else {
      setPromoError('Invalid promo code. Try "CURBPRO2026" or "VIPPARK"');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-xs transition-opacity animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-[#FFFFFF] rounded-t-3xl sm:rounded-3xl border border-[#EDE0DC] shadow-2xl p-6 relative max-h-[90vh] overflow-y-auto">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-9 h-9 rounded-full bg-[#F3E9E5] text-[#1F1B1A] flex items-center justify-center hover:bg-[#EDE0DC] transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header Hero */}
        <div className="flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-2xl bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] font-extrabold uppercase tracking-wider text-[#8F4C38]">
              Upgrade Experience
            </span>
            <h2 className="text-xl font-extrabold text-[#1F1B1A]">Curb Pro</h2>
          </div>
        </div>

        <p className="mt-2.5 text-xs text-[#51433F] leading-relaxed">
          Never get another surprise parking citation. Unlock unlimited scans, instant sign reasoning, and custom alerts.
        </p>

        {/* Feature List */}
        <div className="mt-4 space-y-2 bg-[#FDF8F6] p-3.5 rounded-2xl border border-[#EDE0DC]">
          {[
            { icon: Zap, text: 'Unlimited parking sign scans (vs 10/mo free)' },
            { icon: Sparkles, text: 'Unlimited 24/7 Contextual Copilot Q&A' },
            { icon: Bell, text: 'Custom 5/10/15 min expiry notifications' },
            { icon: Shield, text: 'Tow-away zone & street cleaning early alerts' },
            { icon: FileText, text: 'Official citation defense report export' }
          ].map((f, i) => {
            const Icon = f.icon;
            return (
              <div key={i} className="flex items-center gap-2.5 text-xs font-semibold text-[#1F1B1A]">
                <div className="w-5 h-5 rounded-full bg-[#E8F5E9] text-[#2E7D32] flex items-center justify-center flex-shrink-0">
                  <Check className="w-3.5 h-3.5 stroke-[2.5]" />
                </div>
                <span>{f.text}</span>
              </div>
            );
          })}
        </div>

        {/* Plan Cards */}
        <div className="mt-4 grid grid-cols-2 gap-3">
          <button
            onClick={() => setSelectedPlan('annual')}
            className={`p-3.5 rounded-2xl border text-left transition-all relative ${
              selectedPlan === 'annual'
                ? 'border-[#8F4C38] bg-[#FFDAD1]/30 ring-2 ring-[#8F4C38]'
                : 'border-[#EDE0DC] bg-[#FFFFFF] hover:border-[#D6C2BC]'
            }`}
          >
            <span className="absolute -top-2.5 right-2 px-2 py-0.5 rounded-full text-[9px] font-extrabold bg-[#2E7D32] text-white">
              SAVE 33%
            </span>
            <span className="text-xs font-bold text-[#85736E]">Annual Plan</span>
            <div className="mt-1 flex items-baseline gap-1">
              <span className="text-xl font-black text-[#1F1B1A]">$39.99</span>
              <span className="text-[10px] text-[#85736E]">/yr</span>
            </div>
            <span className="text-[10px] text-[#2E7D32] font-semibold mt-0.5 block">
              $3.33 / month
            </span>
          </button>

          <button
            onClick={() => setSelectedPlan('monthly')}
            className={`p-3.5 rounded-2xl border text-left transition-all ${
              selectedPlan === 'monthly'
                ? 'border-[#8F4C38] bg-[#FFDAD1]/30 ring-2 ring-[#8F4C38]'
                : 'border-[#EDE0DC] bg-[#FFFFFF] hover:border-[#D6C2BC]'
            }`}
          >
            <span className="text-xs font-bold text-[#85736E]">Monthly Plan</span>
            <div className="mt-1 flex items-baseline gap-1">
              <span className="text-xl font-black text-[#1F1B1A]">$4.99</span>
              <span className="text-[10px] text-[#85736E]">/mo</span>
            </div>
            <span className="text-[10px] text-[#85736E] mt-0.5 block">
              Billed monthly
            </span>
          </button>
        </div>

        {/* Action Button */}
        <button
          onClick={handlePurchase}
          className="w-full mt-4 py-3.5 rounded-2xl bg-[#8F4C38] text-white text-sm font-extrabold shadow-md shadow-[#8F4C38]/25 hover:bg-[#3A0B01] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
        >
          <span>Continue with {selectedPlan === 'annual' ? 'Annual' : 'Monthly'}</span>
          <ArrowRight className="w-4 h-4" />
        </button>

        {/* Promo Code Drawer */}
        <div className="mt-4 pt-3 border-t border-[#EDE0DC]">
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Promo or VIP code"
              value={promoCode}
              onChange={(e) => setPromoCode(e.target.value)}
              className="flex-1 text-xs uppercase font-bold px-3 py-2 rounded-xl border border-[#D6C2BC] bg-[#FDF8F6] focus:outline-none focus:border-[#8F4C38]"
            />
            <button
              onClick={handleApplyPromo}
              className="px-3.5 py-2 rounded-xl bg-[#F3E9E5] text-[#1F1B1A] text-xs font-bold hover:bg-[#EDE0DC]"
            >
              Apply
            </button>
          </div>
          {promoError && <p className="text-[11px] text-[#BA1A1A] font-semibold mt-1.5">{promoError}</p>}
          {promoSuccess && <p className="text-[11px] text-[#2E7D32] font-semibold mt-1.5">Promo applied! Pro unlocked.</p>}
        </div>

        <p className="mt-3 text-[10px] text-[#85736E] text-center leading-normal">
          Recurring billing. Cancel anytime in your device settings.
        </p>
      </div>
    </div>
  );
};
