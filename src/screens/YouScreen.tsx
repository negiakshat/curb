import React from 'react';
import {
  User,
  Bell,
  Bookmark,
  CreditCard,
  HelpCircle,
  Shield,
  FileText,
  Info,
  LogOut,
  ChevronRight,
  Sparkles,
  ArrowRight
} from 'lucide-react';
import { UserProfile, AppRoute } from '../types';

interface YouScreenProps {
  userProfile: UserProfile;
  isPro: boolean;
  onNavigate: (route: AppRoute) => void;
  onLogout: () => void;
  onOpenPaywall: () => void;
}

export const YouScreen: React.FC<YouScreenProps> = ({
  userProfile,
  isPro,
  onNavigate,
  onLogout,
  onOpenPaywall
}) => {
  const menuGroups = [
    {
      title: 'Preferences',
      items: [
        { route: 'account_info' as AppRoute, label: 'Account Info', icon: User },
        { route: 'notification_settings' as AppRoute, label: 'Notifications', icon: Bell },
        { route: 'saved_places' as AppRoute, label: 'Saved Places', icon: Bookmark },
        { route: 'payment_subscription' as AppRoute, label: 'Subscription & Quotas', icon: CreditCard }
      ]
    },
    {
      title: 'Support & Legal',
      items: [
        { route: 'help_support' as AppRoute, label: 'Help & Support', icon: HelpCircle },
        { route: 'privacy_policy' as AppRoute, label: 'Privacy Policy', icon: Shield },
        { route: 'terms_of_service' as AppRoute, label: 'Terms of Service', icon: FileText },
        { route: 'about_curb' as AppRoute, label: 'About Curb', icon: Info }
      ]
    }
  ];

  return (
    <div className="min-h-screen bg-[#FDF8F6] pb-24 max-w-md mx-auto">
      {/* Top Header */}
      <div className="sticky top-0 z-30 bg-[#FDF8F6]/95 backdrop-blur-md px-5 pt-4 pb-3 border-b border-[#EDE0DC]">
        <h2 className="text-xl font-extrabold text-[#1F1B1A]">
          You & Settings
        </h2>
      </div>

      <div className="p-5 space-y-4">
        {/* Profile Card */}
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs flex items-center justify-between">
          <div className="flex items-center gap-3.5">
            <div className="w-14 h-14 rounded-full bg-[#FFDAD1] border-2 border-[#8F4C38] text-[#8F4C38] font-black text-xl flex items-center justify-center">
              {userProfile.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-extrabold text-base text-[#1F1B1A]">
                  {userProfile.name}
                </h3>
                {isPro && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-[#8F4C38] text-white">
                    PRO
                  </span>
                )}
              </div>
              <p className="text-xs text-[#85736E] mt-0.5">
                {userProfile.email || 'Free Member'}
              </p>
            </div>
          </div>

          <button
            onClick={() => onNavigate('account_info')}
            className="text-xs font-bold text-[#8F4C38] hover:underline"
          >
            Edit
          </button>
        </div>

        {/* Upgrade Banner (if not Pro) */}
        {!isPro && (
          <div
            onClick={onOpenPaywall}
            className="rounded-3xl bg-[#8F4C38] text-white p-5 shadow-md shadow-[#8F4C38]/20 cursor-pointer relative overflow-hidden group transition-all hover:scale-[1.01]"
          >
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-[#FFDAD1] text-[#8F4C38]">
                  Curb Pro
                </span>
                <h4 className="font-black text-base mt-1">Unlock Unlimited Scans</h4>
                <p className="text-xs text-white/80">
                  Instant citations defense & 24/7 copilot Q&A
                </p>
              </div>
              <div className="w-10 h-10 rounded-2xl bg-white/20 flex items-center justify-center flex-shrink-0">
                <Sparkles className="w-5 h-5 text-[#FFDAD1]" />
              </div>
            </div>
          </div>
        )}

        {/* Settings Menu Groups */}
        {menuGroups.map((group, gIdx) => (
          <div key={gIdx} className="space-y-2">
            <span className="text-[11px] font-extrabold uppercase tracking-wider text-[#85736E] px-2 block">
              {group.title}
            </span>

            <div className="bg-white border border-[#EDE0DC] rounded-3xl divide-y divide-[#EDE0DC] shadow-xs overflow-hidden">
              {group.items.map((item) => {
                const Icon = item.icon;
                return (
                  <button
                    key={item.route}
                    onClick={() => onNavigate(item.route)}
                    className="w-full p-4 flex items-center justify-between hover:bg-[#FDF8F6] transition-colors text-left"
                  >
                    <div className="flex items-center gap-3.5">
                      <div className="w-8 h-8 rounded-xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center">
                        <Icon className="w-4 h-4" />
                      </div>
                      <span className="text-xs font-bold text-[#1F1B1A]">
                        {item.label}
                      </span>
                    </div>

                    <ChevronRight className="w-4 h-4 text-[#85736E]" />
                  </button>
                );
              })}
            </div>
          </div>
        ))}

        {/* Logout Button */}
        <div className="pt-2">
          <button
            onClick={onLogout}
            className="w-full p-4 rounded-3xl bg-white border border-[#EDE0DC] text-[#BA1A1A] text-xs font-bold hover:bg-[#FFDAD6]/30 flex items-center justify-center gap-2 transition-colors shadow-xs"
          >
            <LogOut className="w-4 h-4" />
            <span>Reset Data & Log Out</span>
          </button>
        </div>
      </div>
    </div>
  );
};
