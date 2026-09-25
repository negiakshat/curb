import React, { useState } from 'react';
import { ArrowLeft, ArrowRight, User } from 'lucide-react';

interface NameSetupScreenProps {
  currentName: string;
  onNameSubmitted: (name: string) => void;
  onBack: () => void;
}

export const NameSetupScreen: React.FC<NameSetupScreenProps> = ({
  currentName,
  onNameSubmitted,
  onBack
}) => {
  const [name, setName] = useState(currentName || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (name.trim()) {
      onNameSubmitted(name.trim());
    }
  };

  return (
    <div className="min-h-screen bg-[#FDF8F6] flex flex-col justify-between p-6 max-w-md mx-auto">
      {/* Top App Bar */}
      <div className="flex items-center justify-between pt-2">
        <button
          onClick={onBack}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] flex items-center justify-center text-[#1F1B1A] hover:bg-[#F3E9E5]"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <span className="text-xs font-extrabold uppercase tracking-wider text-[#8F4C38]">
          Step 1 of 2
        </span>
        <div className="w-11" />
      </div>

      {/* Main Content */}
      <div className="my-auto py-8">
        <div className="w-14 h-14 rounded-2xl bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center mb-6">
          <User className="w-7 h-7" />
        </div>

        <h2 className="text-2xl font-black text-[#1F1B1A] tracking-tight">
          What should we call you?
        </h2>
        <p className="mt-2 text-sm text-[#51433F]">
          Curb uses your name to personalize parking alerts and notifications.
        </p>

        <form onSubmit={handleSubmit} className="mt-8">
          <label className="text-xs font-bold uppercase tracking-wider text-[#85736E] block mb-2">
            Your First Name
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Alex"
            autoFocus
            className="w-full text-lg font-bold p-4 rounded-2xl border-2 border-[#EDE0DC] bg-[#FFFFFF] text-[#1F1B1A] focus:outline-none focus:border-[#8F4C38] transition-colors shadow-xs"
          />
        </form>
      </div>

      {/* Bottom Button */}
      <div className="pt-4">
        <button
          onClick={() => handleSubmit({ preventDefault: () => {} } as any)}
          disabled={!name.trim()}
          className="w-full py-4 rounded-2xl bg-[#8F4C38] disabled:opacity-50 text-white font-extrabold text-base shadow-lg shadow-[#8F4C38]/25 hover:bg-[#3A0B01] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
        >
          <span>Continue</span>
          <ArrowRight className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
