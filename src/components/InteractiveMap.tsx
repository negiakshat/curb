import React, { useState } from 'react';
import { Navigation, MapPin, Compass, Car, Share2, ExternalLink, RefreshCw } from 'lucide-react';
import { ParkingSpot } from '../types';
import { formatDistance, estimateWalkingMinutes, calculateDistanceMeters, UserLocation } from '../services/location';

interface InteractiveMapProps {
  carSpot: ParkingSpot;
  userLoc: UserLocation | null;
  onRefreshLocation: () => void;
  isRefreshing?: boolean;
}

export const InteractiveMap: React.FC<InteractiveMapProps> = ({
  carSpot,
  userLoc,
  onRefreshLocation,
  isRefreshing = false
}) => {
  const [copyFeedback, setCopyFeedback] = useState(false);

  // Compute live distance
  const currentLat = userLoc?.latitude || 37.7925;
  const currentLng = userLoc?.longitude || -122.4044;
  const distanceMeters = calculateDistanceMeters(
    currentLat,
    currentLng,
    carSpot.latitude,
    carSpot.longitude
  );

  const walkingMins = estimateWalkingMinutes(distanceMeters);

  const handleShare = () => {
    const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${carSpot.latitude},${carSpot.longitude}`;
    if (navigator.share) {
      navigator.share({
        title: `My Parked Car at ${carSpot.locationName}`,
        text: `I parked here: ${carSpot.locationName}. Navigate back to spot:`,
        url: mapsUrl
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(mapsUrl);
      setCopyFeedback(true);
      setTimeout(() => setCopyFeedback(false), 2000);
    }
  };

  const handleOpenMaps = () => {
    const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${carSpot.latitude},${carSpot.longitude}&travelmode=walking`;
    window.open(mapsUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="space-y-4">
      {/* Map Canvas Card */}
      <div className="relative w-full h-72 rounded-3xl overflow-hidden border border-[#EDE0DC] shadow-inner bg-[#F3E9E5]">
        {/* Stylized OpenStreetMap Tile or Architectural Grid representation */}
        <div className="absolute inset-0 bg-[#F9EEE8] bg-[radial-gradient(#D6C2BC_1px,transparent_1px)] [background-size:16px_16px]">
          {/* Street grid line graphics */}
          <svg className="w-full h-full opacity-60" viewBox="0 0 400 300" preserveAspectRatio="none">
            <line x1="0" y1="80" x2="400" y2="80" stroke="#EDE0DC" strokeWidth="12" />
            <line x1="0" y1="190" x2="400" y2="190" stroke="#EDE0DC" strokeWidth="16" />
            <line x1="120" y1="0" x2="120" y2="300" stroke="#EDE0DC" strokeWidth="14" />
            <line x1="280" y1="0" x2="280" y2="300" stroke="#EDE0DC" strokeWidth="12" />
            {/* Walking dotted path between pins */}
            <path
              d="M 120 220 Q 200 180 280 110"
              fill="none"
              stroke="#8F4C38"
              strokeWidth="4"
              strokeDasharray="6 6"
              className="animate-pulse"
            />
          </svg>

          {/* User Location Pin */}
          <div className="absolute left-[30%] top-[72%] -translate-x-1/2 -translate-y-1/2 flex flex-col items-center">
            <div className="w-9 h-9 rounded-full bg-[#1F1B1A] text-white flex items-center justify-center shadow-lg border-2 border-white ring-4 ring-black/10">
              <Navigation className="w-4 h-4 fill-white -rotate-45" />
            </div>
            <span className="text-[10px] font-extrabold bg-[#FFFFFF]/90 px-2 py-0.5 rounded-full shadow-xs text-[#1F1B1A] mt-1 whitespace-nowrap">
              You (Live)
            </span>
          </div>

          {/* Parked Car Pin */}
          <div className="absolute left-[70%] top-[35%] -translate-x-1/2 -translate-y-1/2 flex flex-col items-center animate-bounce-slow">
            <div className="w-12 h-12 rounded-full bg-[#8F4C38] text-white flex items-center justify-center shadow-xl border-3 border-white ring-4 ring-[#8F4C38]/20">
              <Car className="w-6 h-6 stroke-[2.2]" />
            </div>
            <span className="text-[10px] font-extrabold bg-[#8F4C38] text-white px-2 py-0.5 rounded-full shadow-xs mt-1 whitespace-nowrap">
              Parked Car
            </span>
          </div>

          {/* Map Controls Floating Badge */}
          <div className="absolute top-3 left-3 bg-[#FFFFFF]/90 backdrop-blur-xs px-3 py-1.5 rounded-2xl border border-[#EDE0DC] shadow-sm flex items-center gap-2">
            <Compass className="w-4 h-4 text-[#8F4C38]" />
            <span className="text-xs font-bold text-[#1F1B1A]">
              {walkingMins} min walk • {formatDistance(distanceMeters)}
            </span>
          </div>

          <button
            onClick={onRefreshLocation}
            disabled={isRefreshing}
            className="absolute top-3 right-3 w-9 h-9 rounded-full bg-[#FFFFFF]/90 backdrop-blur-xs text-[#1F1B1A] flex items-center justify-center border border-[#EDE0DC] shadow-sm hover:bg-white active:scale-95 transition-all"
            title="Refresh GPS location"
          >
            <RefreshCw className={`w-4 h-4 text-[#8F4C38] ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Spot Information Bar */}
      <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-4 shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <span className="text-[11px] font-bold uppercase tracking-wider text-[#85736E]">
              Parked Spot Address
            </span>
            <h3 className="font-extrabold text-base text-[#1F1B1A] mt-0.5">
              {carSpot.locationName || 'Saved Parking Spot'}
            </h3>
            <p className="text-xs text-[#85736E] mt-0.5">
              Saved {new Date(carSpot.timestamp).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} • GPS Acc. ±{carSpot.accuracy || 12}m
            </p>
          </div>

          <div className="text-right">
            <span className="text-xs font-black text-[#8F4C38] block">
              {formatDistance(distanceMeters)}
            </span>
            <span className="text-[10px] text-[#2E7D32] font-bold">
              ~{walkingMins} min walking
            </span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="mt-4 grid grid-cols-2 gap-2.5 pt-3 border-t border-[#EDE0DC]">
          <button
            onClick={handleShare}
            className="py-2.5 px-3 rounded-xl border border-[#EDE0DC] bg-[#FDF8F6] text-[#1F1B1A] text-xs font-bold hover:bg-[#F3E9E5] flex items-center justify-center gap-1.5 transition-colors"
          >
            <Share2 className="w-3.5 h-3.5 text-[#8F4C38]" />
            <span>{copyFeedback ? 'Link Copied!' : 'Share Spot'}</span>
          </button>

          <button
            onClick={handleOpenMaps}
            className="py-2.5 px-3 rounded-xl bg-[#8F4C38] text-white text-xs font-bold hover:bg-[#3A0B01] shadow-sm flex items-center justify-center gap-1.5 transition-colors"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            <span>Walk Directions</span>
          </button>
        </div>
      </div>
    </div>
  );
};
