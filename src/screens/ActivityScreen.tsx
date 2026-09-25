import React, { useState } from 'react';
import {
  ArrowLeft,
  Clock,
  Search,
  Filter,
  ShieldCheck,
  ChevronRight,
  Sparkles
} from 'lucide-react';
import { ScanResult, ScanVerdict } from '../types';
import { getVerdictStyles } from '../theme/colors';

interface ActivityScreenProps {
  scans: ScanResult[];
  isPro: boolean;
  onSelectScan: (scan: ScanResult) => void;
  onUpgradePro: () => void;
  onBack: () => void;
}

export const ActivityScreen: React.FC<ActivityScreenProps> = ({
  scans,
  isPro,
  onSelectScan,
  onUpgradePro,
  onBack
}) => {
  const [filter, setFilter] = useState<'ALL' | ScanVerdict>('ALL');
  const [search, setSearch] = useState('');

  const filteredScans = scans.filter((scan) => {
    const matchesFilter = filter === 'ALL' || scan.verdict === filter;
    const matchesSearch =
      !search ||
      scan.locationName.toLowerCase().includes(search.toLowerCase()) ||
      scan.explanation.toLowerCase().includes(search.toLowerCase());
    return matchesFilter && matchesSearch;
  });

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
          Scan Activity
        </h2>

        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4">
        {/* Search Bar */}
        <div className="relative">
          <Search className="w-4 h-4 text-[#85736E] absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search scans by street or rule..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-3 rounded-2xl bg-white border border-[#EDE0DC] text-xs font-medium text-[#1F1B1A] focus:outline-none focus:border-[#8F4C38] shadow-2xs"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
          {(['ALL', 'ALLOWED', 'RESTRICTED', 'AMBIGUOUS'] as const).map((v) => (
            <button
              key={v}
              onClick={() => setFilter(v)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-all ${
                filter === v
                  ? 'bg-[#8F4C38] text-white shadow-xs'
                  : 'bg-white text-[#51433F] border border-[#EDE0DC] hover:bg-[#F3E9E5]'
              }`}
            >
              {v === 'ALL' ? 'All Scans' : v}
            </button>
          ))}
        </div>

        {/* Pro Banner */}
        {!isPro && (
          <div
            onClick={onUpgradePro}
            className="p-3.5 rounded-2xl bg-[#FFDAD1]/50 border border-[#EDE0DC] flex items-center justify-between cursor-pointer hover:bg-[#FFDAD1]/70"
          >
            <div className="flex items-center gap-2.5">
              <Sparkles className="w-4 h-4 text-[#8F4C38]" />
              <span className="text-xs font-bold text-[#1F1B1A]">
                Export citation evidence with Curb Pro
              </span>
            </div>
            <span className="text-xs font-extrabold text-[#8F4C38]">&rarr;</span>
          </div>
        )}

        {/* Scans List */}
        {filteredScans.length === 0 ? (
          <div className="bg-white border border-[#EDE0DC] rounded-3xl p-8 text-center my-6 space-y-2">
            <Clock className="w-10 h-10 text-[#85736E] mx-auto opacity-50" />
            <h4 className="font-extrabold text-sm text-[#1F1B1A]">No scans match</h4>
            <p className="text-xs text-[#85736E]">
              Try a different filter or search query.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredScans.map((scan) => {
              const styles = getVerdictStyles(scan.verdict);
              return (
                <div
                  key={scan.id}
                  onClick={() => onSelectScan(scan)}
                  className="bg-white border border-[#EDE0DC] rounded-3xl p-4 shadow-xs hover:border-[#D6C2BC] cursor-pointer transition-all space-y-2"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${styles.badge}`}>
                        {scan.verdict}
                      </span>
                      <h3 className="font-extrabold text-sm text-[#1F1B1A] mt-1.5">
                        {scan.locationName}
                      </h3>
                      <p className="text-[11px] text-[#85736E]">
                        {new Date(scan.timestamp).toLocaleString([], {
                          dateStyle: 'short',
                          timeStyle: 'short'
                        })}
                      </p>
                    </div>

                    <ChevronRight className="w-5 h-5 text-[#85736E]" />
                  </div>

                  <p className="text-xs text-[#51433F] line-clamp-2 leading-relaxed bg-[#FDF8F6] p-2.5 rounded-xl border border-[#EDE0DC]">
                    {scan.explanation}
                  </p>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
