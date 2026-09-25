import React, { useState } from 'react';
import { ArrowLeft, User, Mail, Trash2, Check } from 'lucide-react';
import { UserProfile } from '../types';

interface AccountInfoScreenProps {
  userProfile: UserProfile;
  onSaveProfile: (name: string, gender: string, email: string) => void;
  onDeleteAccount: () => void;
  onBack: () => void;
}

export const AccountInfoScreen: React.FC<AccountInfoScreenProps> = ({
  userProfile,
  onSaveProfile,
  onDeleteAccount,
  onBack
}) => {
  const [name, setName] = useState(userProfile.name);
  const [gender, setGender] = useState(userProfile.gender);
  const [email, setEmail] = useState(userProfile.email);
  const [savedFeedback, setSavedFeedback] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    onSaveProfile(name, gender, email);
    setSavedFeedback(true);
    setTimeout(() => setSavedFeedback(false), 2000);
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
          Account Info
        </h2>

        <div className="w-11" />
      </div>

      <form onSubmit={handleSave} className="p-5 space-y-4">
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-4">
          <div>
            <label className="text-xs font-bold uppercase tracking-wider text-[#85736E] block mb-1.5">
              Display Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full text-sm font-semibold p-3.5 rounded-2xl border border-[#D6C2BC] bg-[#FDF8F6] text-[#1F1B1A] focus:outline-none focus:border-[#8F4C38]"
              required
            />
          </div>

          <div>
            <label className="text-xs font-bold uppercase tracking-wider text-[#85736E] block mb-1.5">
              Email Address
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="alex.driver@example.com"
              className="w-full text-sm font-semibold p-3.5 rounded-2xl border border-[#D6C2BC] bg-[#FDF8F6] text-[#1F1B1A] focus:outline-none focus:border-[#8F4C38]"
            />
          </div>

          <div>
            <label className="text-xs font-bold uppercase tracking-wider text-[#85736E] block mb-1.5">
              Pronouns / Gender (Optional)
            </label>
            <input
              type="text"
              value={gender}
              onChange={(e) => setGender(e.target.value)}
              placeholder="Not specified"
              className="w-full text-sm font-semibold p-3.5 rounded-2xl border border-[#D6C2BC] bg-[#FDF8F6] text-[#1F1B1A] focus:outline-none focus:border-[#8F4C38]"
            />
          </div>
        </div>

        <button
          type="submit"
          className="w-full py-4 rounded-2xl bg-[#8F4C38] text-white font-extrabold text-sm shadow-md hover:bg-[#3A0B01] transition-all flex items-center justify-center gap-2"
        >
          {savedFeedback ? (
            <>
              <Check className="w-4 h-4" />
              <span>Saved!</span>
            </>
          ) : (
            <span>Save Profile</span>
          )}
        </button>

        <div className="pt-6">
          <button
            type="button"
            onClick={onDeleteAccount}
            className="w-full py-3 rounded-2xl border border-[#FFDAD6] bg-white text-[#BA1A1A] text-xs font-bold hover:bg-[#FFDAD6]/30 flex items-center justify-center gap-2"
          >
            <Trash2 className="w-4 h-4" />
            <span>Delete Account & Clear Local Data</span>
          </button>
        </div>
      </form>
    </div>
  );
};
