import React, { useState } from 'react';
import {
  ArrowLeft,
  Bookmark,
  Camera,
  Trash2,
  ExternalLink,
  MapPin,
  Clock,
  RotateCcw,
  Plus
} from 'lucide-react';
import { SavedPlace } from '../types';

interface SavedPlacesScreenProps {
  savedPlaces: SavedPlace[];
  isPro: boolean;
  onDeletePlace: (id: number) => void;
  onCheckSignAgain: (place: SavedPlace) => void;
  onOpenScan: () => void;
  onUpgradePro: () => void;
  onBack: () => void;
}

export const SavedPlacesScreen: React.FC<SavedPlacesScreenProps> = ({
  savedPlaces,
  isPro,
  onDeletePlace,
  onCheckSignAgain,
  onOpenScan,
  onUpgradePro,
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
          Saved Places
        </h2>

        <button
          onClick={onOpenScan}
          className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#8F4C38] flex items-center justify-center hover:bg-[#F3E9E5] transition-colors"
          title="Add place by scanning"
        >
          <Plus className="w-5 h-5" />
        </button>
      </div>

      <div className="p-5 space-y-4">
        {savedPlaces.length === 0 ? (
          <div className="bg-white border border-[#EDE0DC] rounded-3xl p-8 text-center my-6 space-y-3">
            <div className="w-14 h-14 rounded-2xl bg-[#F3E9E5] text-[#8F4C38] flex items-center justify-center mx-auto">
              <Bookmark className="w-7 h-7" />
            </div>
            <h3 className="font-extrabold text-base text-[#1F1B1A]">No saved places yet</h3>
            <p className="text-xs text-[#85736E] max-w-xs mx-auto">
              Save frequent parking spots after scanning to monitor street sweeping dates and regular restrictions.
            </p>
            <button
              onClick={onOpenScan}
              className="mt-2 py-3 px-6 rounded-2xl bg-[#8F4C38] text-white text-xs font-bold shadow-md hover:bg-[#3A0B01]"
            >
              Scan a Spot to Save
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {savedPlaces.map((place) => (
              <div
                key={place.id}
                className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-3"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-black text-base text-[#1F1B1A]">
                      {place.name}
                    </h3>
                    <p className="text-xs text-[#85736E] flex items-center gap-1 mt-0.5">
                      <MapPin className="w-3.5 h-3.5 text-[#8F4C38]" />
                      <span>{place.address}</span>
                    </p>
                  </div>

                  <button
                    onClick={() => onDeletePlace(place.id)}
                    className="p-1.5 rounded-lg text-[#85736E] hover:text-[#BA1A1A] hover:bg-[#FFDAD6] transition-colors"
                    title="Delete spot"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>

                {place.parkingRuleSummary && (
                  <div className="p-3 bg-[#FDF8F6] rounded-2xl border border-[#EDE0DC] text-xs font-medium text-[#51433F]">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-[#85736E] block mb-0.5">
                      Cached Parking Rules
                    </span>
                    {place.parkingRuleSummary}
                  </div>
                )}

                {place.parkingNote && (
                  <p className="text-xs italic text-[#51433F]">
                    &ldquo;{place.parkingNote}&rdquo;
                  </p>
                )}

                <div className="pt-2 flex items-center gap-2 border-t border-[#EDE0DC]">
                  <button
                    onClick={() => onCheckSignAgain(place)}
                    className="flex-1 py-2 px-3 rounded-xl bg-[#F3E9E5] text-[#1F1B1A] text-xs font-bold hover:bg-[#EDE0DC] flex items-center justify-center gap-1.5 transition-colors"
                  >
                    <RotateCcw className="w-3.5 h-3.5 text-[#8F4C38]" />
                    <span>Check Sign Now</span>
                  </button>

                  {place.latitude && place.longitude && (
                    <button
                      onClick={() => {
                        window.open(
                          `https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}`,
                          '_blank',
                          'noopener,noreferrer'
                        );
                      }}
                      className="py-2 px-3 rounded-xl border border-[#EDE0DC] text-xs font-bold text-[#1F1B1A] hover:bg-[#FDF8F6] flex items-center justify-center gap-1.5 transition-colors"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>Maps</span>
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
