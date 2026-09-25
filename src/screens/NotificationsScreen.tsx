import React from 'react';
import { ArrowLeft, Bell, Settings, Clock, CheckCheck } from 'lucide-react';
import { InAppNotification } from '../types';

interface NotificationsScreenProps {
  notifications: InAppNotification[];
  onNotificationClick: (sessionId?: number) => void;
  onOpenSettings: () => void;
  onMarkAllRead: () => void;
  onBack: () => void;
}

export const NotificationsScreen: React.FC<NotificationsScreenProps> = ({
  notifications,
  onNotificationClick,
  onOpenSettings,
  onMarkAllRead,
  onBack
}) => {
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
          Notifications
        </h2>

        <button
          onClick={onOpenSettings}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center hover:bg-[#F3E9E5] transition-colors"
        >
          <Settings className="w-5 h-5 text-[#51433F]" />
        </button>
      </div>

      <div className="p-5 space-y-4">
        {notifications.some((n) => !n.isRead) && (
          <div className="flex justify-end">
            <button
              onClick={onMarkAllRead}
              className="text-xs font-bold text-[#8F4C38] hover:underline flex items-center gap-1"
            >
              <CheckCheck className="w-3.5 h-3.5" />
              <span>Mark all as read</span>
            </button>
          </div>
        )}

        {notifications.length === 0 ? (
          <div className="bg-white border border-[#EDE0DC] rounded-3xl p-8 text-center my-6 space-y-2">
            <Bell className="w-10 h-10 text-[#85736E] mx-auto opacity-50" />
            <h4 className="font-extrabold text-sm text-[#1F1B1A]">No notifications yet</h4>
            <p className="text-xs text-[#85736E]">
              You'll receive alerts before your parking meter or active street window expires.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {notifications.map((n) => (
              <div
                key={n.id}
                onClick={() => onNotificationClick(n.sessionId)}
                className={`p-4 rounded-3xl border transition-all cursor-pointer shadow-xs ${
                  !n.isRead
                    ? 'bg-white border-[#8F4C38]/40 ring-1 ring-[#8F4C38]/20'
                    : 'bg-white border-[#EDE0DC] hover:border-[#D6C2BC]'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[#8F4C38] flex-shrink-0" />
                    <h4 className="font-extrabold text-xs text-[#1F1B1A]">
                      {n.title}
                    </h4>
                  </div>
                  <span className="text-[10px] text-[#85736E]">
                    {new Date(n.timestamp).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}
                  </span>
                </div>

                <p className="text-xs text-[#51433F] mt-1.5 leading-relaxed pl-4">
                  {n.body}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
