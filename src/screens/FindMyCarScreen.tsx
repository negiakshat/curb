import React, { useState } from 'react';
import { ArrowLeft, Car, MapPin, RefreshCw, Trash2, Plus, Clock } from 'lucide-react';
import { ParkingSpot } from '../types';
import { InteractiveMap } from '../components/InteractiveMap';
import { UserLocation } from '../services/location';

interface FindMyCarScreenProps {
  savedSpot: ParkingSpot | null;
  userLoc: UserLocation | null;
  onRefreshLocation: () => void;
  onSaveCurrentLocation: () => void;
  onClearSpot: () => void;
  onNavigateToTimer: () => void;
  onBack: () => void;
}

export const FindMyCarScreen: React.FC<FindMyCarScreenProps> = ({
  savedSpot,
  userLoc,
  onRefreshLocation,
  onSaveCurrentLocation,
  onClearSpot,
  onNavigateToTimer,
  onBack
}) => {
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await onRefreshLocation();
    setTimeout(() => setIsRefreshing(false), 800);
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
          Find My Car
        </h2>

        {savedSpot ? (
          <button
            onClick={onClearSpot}
            className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#BA1A1A] flex items-center justify-center hover:bg-[#FFDAD6] transition-colors"
            title="Forget parked spot"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        ) : (
          <div className="w-11" />
        )}
      </div>

      <div className="p-5 space-y-4">
        {savedSpot ? (
          <>
            {/* Interactive Map */}
            <InteractiveMap
              carSpot={savedSpot}
              userLoc={userLoc}
              onRefreshLocation={handleRefresh}
              isRefreshing={isRefreshing}
            />

            {/* GPS Metadata Details */}
            <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-4 shadow-xs space-y-2.5">
              <span className="text-[11px] font-bold uppercase tracking-wider text-[#85736E]">
                Precision Geolocation
              </span>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 bg-[#FDF8F6] rounded-2xl border border-[#EDE0DC]">
                  <span className="text-[#85736E] block text-[10px]">Latitude</span>
                  <span className="font-mono font-bold text-[#1F1B1A]">{savedSpot.latitude.toFixed(6)}</span>
                </div>
                <div className="p-3 bg-[#FDF8F6] rounded-2xl border border-[#EDE0DC]">
                  <span className="text-[#85736E] block text-[10px]">Longitude</span>
                  <span className="font-mono font-bold text-[#1F1B1A]">{savedSpot.longitude.toFixed(6)}</span>
                </div>
              </div>
            </div>

            {/* Link to Timer */}
            <button
              onClick={onNavigateToTimer}
              className="w-full py-3.5 rounded-2xl bg-[#FFFFFF] border border-[#EDE0DC] text-xs font-bold text-[#1F1B1A] hover:bg-[#F3E9E5] shadow-xs flex items-center justify-center gap-2"
            >
              <Clock className="w-4 h-4 text-[#8F4C38]" />
              <span>Check Parking Timer & Rules</span>
            </button>
          </>
        ) : (
          /* Empty State when no spot is pinned */
          <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-8 text-center space-y-4 my-8">
            <div className="w-16 h-16 rounded-3xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mx-auto">
              <Car className="w-8 h-8" />
            </div>

            <div>
              <h3 className="font-black text-lg text-[#1F1B1A]">
                No Parked Spot Saved
              </h3>
              <p className="text-xs text-[#85736E] mt-1 max-w-xs mx-auto">
                Save your spot when you park to see live walking directions and step-by-step guidance back to your car.
              </p>
            </div>

            <button
              onClick={onSaveCurrentLocation}
              className="py-3.5 px-6 rounded-2xl bg-[#8F4C38] text-white text-xs font-extrabold shadow-md hover:bg-[#3A0B01] transition-all flex items-center justify-center gap-2 mx-auto"
            >
              <MapPin className="w-4 h-4" />
              <span>Pin Current Location</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
