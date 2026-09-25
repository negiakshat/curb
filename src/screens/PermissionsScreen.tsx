import React, { useState } from 'react';
import { ArrowLeft, ArrowRight, Camera, MapPin, Bell, CheckCircle2 } from 'lucide-react';

interface PermissionsScreenProps {
  onComplete: () => void;
  onBack: () => void;
}

export const PermissionsScreen: React.FC<PermissionsScreenProps> = ({
  onComplete,
  onBack
}) => {
  const [cameraEnabled, setCameraEnabled] = useState(true);
  const [locationEnabled, setLocationEnabled] = useState(true);
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);

  const requestPermissions = async () => {
    // If browser supports notification permission, ask gently
    if ('Notification' in window && notificationsEnabled) {
      try {
        await Notification.requestPermission();
      } catch {}
    }
    onComplete();
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
          Step 2 of 2
        </span>
        <div className="w-11" />
      </div>

      {/* Main Content */}
      <div className="my-auto py-6 space-y-6">
        <div>
          <h2 className="text-2xl font-black text-[#1F1B1A] tracking-tight">
            Enable Key Features
          </h2>
          <p className="mt-2 text-sm text-[#51433F]">
            Curb needs access to provide fast sign recognition, spot tracking, and timer notifications.
          </p>
        </div>

        <div className="space-y-3">
          {/* Camera Permission Item */}
          <div
            onClick={() => setCameraEnabled(!cameraEnabled)}
            className="p-4 rounded-2xl bg-white border border-[#EDE0DC] shadow-xs flex items-center justify-between cursor-pointer hover:border-[#D6C2BC] transition-colors"
          >
            <div className="flex items-center gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center">
                <Camera className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-extrabold text-[#1F1B1A]">Camera Access</h4>
                <p className="text-xs text-[#85736E]">To snap and detect parking signs in real time</p>
              </div>
            </div>
            <div className={`w-6 h-6 rounded-full flex items-center justify-center transition-colors ${
              cameraEnabled ? 'bg-[#8F4C38] text-white' : 'border-2 border-[#D6C2BC]'
            }`}>
              {cameraEnabled && <CheckCircle2 className="w-4 h-4" />}
            </div>
          </div>

          {/* Location Permission Item */}
          <div
            onClick={() => setLocationEnabled(!locationEnabled)}
            className="p-4 rounded-2xl bg-white border border-[#EDE0DC] shadow-xs flex items-center justify-between cursor-pointer hover:border-[#D6C2BC] transition-colors"
          >
            <div className="flex items-center gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center">
                <MapPin className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-extrabold text-[#1F1B1A]">Location Access</h4>
                <p className="text-xs text-[#85736E]">Pinpoint where you parked and municipal rules</p>
              </div>
            </div>
            <div className={`w-6 h-6 rounded-full flex items-center justify-center transition-colors ${
              locationEnabled ? 'bg-[#8F4C38] text-white' : 'border-2 border-[#D6C2BC]'
            }`}>
              {locationEnabled && <CheckCircle2 className="w-4 h-4" />}
            </div>
          </div>

          {/* Notifications Permission Item */}
          <div
            onClick={() => setNotificationsEnabled(!notificationsEnabled)}
            className="p-4 rounded-2xl bg-white border border-[#EDE0DC] shadow-xs flex items-center justify-between cursor-pointer hover:border-[#D6C2BC] transition-colors"
          >
            <div className="flex items-center gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center">
                <Bell className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-extrabold text-[#1F1B1A]">Notifications</h4>
                <p className="text-xs text-[#85736E]">Timely alerts before meters or tow windows expire</p>
              </div>
            </div>
            <div className={`w-6 h-6 rounded-full flex items-center justify-center transition-colors ${
              notificationsEnabled ? 'bg-[#8F4C38] text-white' : 'border-2 border-[#D6C2BC]'
            }`}>
              {notificationsEnabled && <CheckCircle2 className="w-4 h-4" />}
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Button */}
      <div className="pt-4">
        <button
          onClick={requestPermissions}
          className="w-full py-4 rounded-2xl bg-[#8F4C38] text-white font-extrabold text-base shadow-lg shadow-[#8F4C38]/25 hover:bg-[#3A0B01] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
        >
          <span>Complete Setup</span>
          <ArrowRight className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
