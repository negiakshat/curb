import React, { useState } from 'react';
import { ArrowLeft, Bell, Clock, ShieldAlert, Check } from 'lucide-react';

interface NotificationSettingsScreenProps {
  pushEnabled: boolean;
  onTogglePush: (enabled: boolean) => void;
  onBack: () => void;
}

export const NotificationSettingsScreen: React.FC<NotificationSettingsScreenProps> = ({
  pushEnabled,
  onTogglePush,
  onBack
}) => {
  const [meterAlerts, setMeterAlerts] = useState(true);
  const [streetSweeping, setStreetSweeping] = useState(true);
  const [towAwayAlerts, setTowAwayAlerts] = useState(true);

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
          Notification Settings
        </h2>

        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4">
        {/* Master Push Toggle */}
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center">
              <Bell className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-extrabold text-sm text-[#1F1B1A]">Push Notifications</h4>
              <p className="text-xs text-[#85736E]">Allow timer & reminder alerts</p>
            </div>
          </div>

          <button
            onClick={() => onTogglePush(!pushEnabled)}
            className={`w-12 h-7 rounded-full p-1 transition-colors ${
              pushEnabled ? 'bg-[#8F4C38]' : 'bg-[#EDE0DC]'
            }`}
          >
            <div
              className={`w-5 h-5 rounded-full bg-white transition-transform ${
                pushEnabled ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* Detailed Notification Rules */}
        <div className="bg-white border border-[#EDE0DC] rounded-3xl divide-y divide-[#EDE0DC] shadow-xs overflow-hidden">
          <div
            onClick={() => setMeterAlerts(!meterAlerts)}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-[#FDF8F6]"
          >
            <div className="flex items-center gap-3">
              <Clock className="w-4 h-4 text-[#8F4C38]" />
              <div>
                <h5 className="text-xs font-bold text-[#1F1B1A]">Meter Expiry Warning</h5>
                <p className="text-[11px] text-[#85736E]">15 minutes before meter cut-off</p>
              </div>
            </div>
            <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
              meterAlerts ? 'bg-[#8F4C38] border-[#8F4C38] text-white' : 'border-[#D6C2BC]'
            }`}>
              {meterAlerts && <Check className="w-3.5 h-3.5" />}
            </div>
          </div>

          <div
            onClick={() => setTowAwayAlerts(!towAwayAlerts)}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-[#FDF8F6]"
          >
            <div className="flex items-center gap-3">
              <ShieldAlert className="w-4 h-4 text-[#BA1A1A]" />
              <div>
                <h5 className="text-xs font-bold text-[#1F1B1A]">Tow-Away Zone Emergency Alert</h5>
                <p className="text-[11px] text-[#85736E]">Urgent notification 30 minutes before tow corridor activates</p>
              </div>
            </div>
            <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
              towAwayAlerts ? 'bg-[#8F4C38] border-[#8F4C38] text-white' : 'border-[#D6C2BC]'
            }`}>
              {towAwayAlerts && <Check className="w-3.5 h-3.5" />}
            </div>
          </div>

          <div
            onClick={() => setStreetSweeping(!streetSweeping)}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-[#FDF8F6]"
          >
            <div className="flex items-center gap-3">
              <Bell className="w-4 h-4 text-[#8F4C38]" />
              <div>
                <h5 className="text-xs font-bold text-[#1F1B1A]">Street Cleaning Reminders</h5>
                <p className="text-[11px] text-[#85736E]">Evening-before notification for street sweeping</p>
              </div>
            </div>
            <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
              streetSweeping ? 'bg-[#8F4C38] border-[#8F4C38] text-white' : 'border-[#D6C2BC]'
            }`}>
              {streetSweeping && <Check className="w-3.5 h-3.5" />}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
