import React from 'react';
import { Home, Camera, Clock, User } from 'lucide-react';
import { AppRoute } from '../types';

interface BottomNavProps {
  currentRoute: AppRoute;
  onNavigate: (route: AppRoute) => void;
  hasActiveSession?: boolean;
}

export const BottomNav: React.FC<BottomNavProps> = ({ currentRoute, onNavigate, hasActiveSession }) => {
  const tabs = [
    { route: 'home' as AppRoute, label: 'Home', icon: Home },
    { route: 'scan' as AppRoute, label: 'Scan', icon: Camera, isPrimary: true },
    { route: 'activity' as AppRoute, label: 'Activity', icon: Clock },
    { route: 'you' as AppRoute, label: 'You', icon: User }
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#FFFFFF]/95 backdrop-blur-md border-t border-[#EDE0DC] max-w-md mx-auto">
      <div className="flex items-center justify-around px-4 py-2">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = currentRoute === tab.route;

          if (tab.isPrimary) {
            return (
              <button
                key={tab.route}
                onClick={() => onNavigate(tab.route)}
                className="relative -top-3 group flex flex-col items-center focus:outline-none"
              >
                <div className="w-14 h-14 rounded-full bg-[#8F4C38] text-white flex items-center justify-center shadow-lg shadow-[#8F4C38]/25 group-active:scale-95 transition-transform border-4 border-[#FDF8F6]">
                  <Icon className="w-6 h-6 stroke-[2.2]" />
                </div>
                <span className="text-[11px] font-semibold text-[#8F4C38] mt-0.5">Scan Sign</span>
              </button>
            );
          }

          return (
            <button
              key={tab.route}
              onClick={() => onNavigate(tab.route)}
              className={`flex flex-col items-center py-1 px-3 rounded-xl transition-colors relative ${
                isActive ? 'text-[#8F4C38]' : 'text-[#85736E] hover:text-[#1F1B1A]'
              }`}
            >
              <div className="relative">
                <Icon className={`w-5 h-5 ${isActive ? 'stroke-[2.4]' : 'stroke-[1.8]'}`} />
                {tab.route === 'activity' && hasActiveSession && (
                  <span className="absolute -top-1 -right-1.5 w-2 h-2 rounded-full bg-[#2E7D32] animate-pulse" />
                )}
              </div>
              <span className={`text-[11px] mt-1 ${isActive ? 'font-bold' : 'font-medium'}`}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
