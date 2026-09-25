import React, { useState, useRef, useEffect } from 'react';
import {
  ArrowLeft,
  HelpCircle,
  Camera,
  Image as ImageIcon,
  Zap,
  ZapOff,
  Sparkles,
  X,
  AlertCircle,
  CheckCircle2,
  Upload
} from 'lucide-react';
import { SampleSignPreset, ScanUsageInfo } from '../types';

interface ScanScreenProps {
  isProcessing: boolean;
  processingStatusText: string;
  scanError: string | null;
  usageInfo: ScanUsageInfo;
  isPro: boolean;
  presets: SampleSignPreset[];
  onCaptureImage: (base64: string) => void;
  onSelectPreset: (preset: SampleSignPreset) => void;
  onClearError: () => void;
  onBack: () => void;
}

export const ScanScreen: React.FC<ScanScreenProps> = ({
  isProcessing,
  processingStatusText,
  scanError,
  usageInfo,
  isPro,
  presets,
  onCaptureImage,
  onSelectPreset,
  onClearError,
  onBack
}) => {
  const [showHelp, setShowHelp] = useState(false);
  const [showPresets, setShowPresets] = useState(false);
  const [flash, setFlash] = useState(false);
  const [cameraActive, setCameraActive] = useState(false);
  const [capturedPreview, setCapturedPreview] = useState<string | null>(null);

  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Initialize camera stream
  useEffect(() => {
    let stream: MediaStream | null = null;
    async function startCamera() {
      try {
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
          stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } }
          });
          if (videoRef.current) {
            videoRef.current.srcObject = stream;
            videoRef.current.play();
            setCameraActive(true);
          }
        }
      } catch (err) {
        console.warn('Camera stream could not be started, photo upload available:', err);
        setCameraActive(false);
      }
    }

    startCamera();

    return () => {
      if (stream) {
        stream.getTracks().forEach((track) => track.stop());
      }
    };
  }, []);

  const handleShutter = () => {
    if (isProcessing) return;

    if (videoRef.current && canvasRef.current && cameraActive) {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      canvas.width = video.videoWidth || 1280;
      canvas.height = video.videoHeight || 720;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
        setCapturedPreview(dataUrl);
        onCaptureImage(dataUrl);
        return;
      }
    }

    // If camera not streaming directly, open file picker
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const result = event.target?.result as string;
        if (result) {
          setCapturedPreview(result);
          onCaptureImage(result);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="relative min-h-screen bg-[#1F1B1A] flex flex-col justify-between overflow-hidden max-w-md mx-auto select-none">
      {/* Hidden canvas for snapshot capture */}
      <canvas ref={canvasRef} className="hidden" />

      {/* Hidden file input for photo upload */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleFileChange}
        className="hidden"
      />

      {/* 1. TOP HUD (Back & Help buttons) */}
      <div className="relative z-30 pt-4 px-5 flex items-center justify-between">
        <button
          onClick={onBack}
          className="w-11 h-11 rounded-full bg-black/40 backdrop-blur-md border border-white/20 text-white flex items-center justify-center hover:bg-black/60 transition-colors shadow-md"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-2">
          {/* Preset Signs Sheet Trigger */}
          <button
            onClick={() => setShowPresets(true)}
            className="px-3 py-1.5 rounded-full bg-white/20 backdrop-blur-md border border-white/30 text-white text-xs font-bold flex items-center gap-1.5 shadow-md hover:bg-white/30 transition-colors"
          >
            <Sparkles className="w-3.5 h-3.5 text-[#FFDAD1]" />
            <span>Sample Signs</span>
          </button>

          <button
            onClick={() => setShowHelp(true)}
            className="w-11 h-11 rounded-full bg-black/40 backdrop-blur-md border border-white/20 text-white flex items-center justify-center hover:bg-black/60 transition-colors shadow-md"
          >
            <HelpCircle className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* 2. CAMERA VIEWFINDER AREA */}
      <div className="absolute inset-0 z-10 flex items-center justify-center">
        {capturedPreview ? (
          <img
            src={capturedPreview}
            alt="Captured parking sign"
            className="w-full h-full object-cover"
          />
        ) : cameraActive ? (
          <video
            ref={videoRef}
            playsInline
            muted
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="text-center p-6 space-y-4 text-white">
            <div className="w-16 h-16 rounded-3xl bg-white/10 mx-auto flex items-center justify-center border border-white/15">
              <Camera className="w-8 h-8 text-[#FFDAD1]" />
            </div>
            <div>
              <h3 className="font-extrabold text-lg">Ready to Scan Sign</h3>
              <p className="text-xs text-white/70 max-w-xs mt-1">
                Upload a photo of parking signs or test with one of our sample municipal sign presets.
              </p>
            </div>
            <button
              onClick={() => fileInputRef.current?.click()}
              className="px-5 py-2.5 rounded-2xl bg-[#8F4C38] text-white text-xs font-bold inline-flex items-center gap-2 shadow-lg"
            >
              <Upload className="w-4 h-4" />
              <span>Select Photo from Device</span>
            </button>
          </div>
        )}

        {/* Viewfinder Target Framing Overlay */}
        <div className="absolute inset-x-8 top-28 bottom-40 border-2 border-white/40 rounded-3xl pointer-events-none flex flex-col justify-between p-4 shadow-[0_0_0_9999px_rgba(0,0,0,0.35)]">
          <div className="flex justify-between">
            <div className="w-6 h-6 border-t-3 border-l-3 border-[#FFDAD1] rounded-tl-xl" />
            <div className="w-6 h-6 border-t-3 border-r-3 border-[#FFDAD1] rounded-tr-xl" />
          </div>

          {/* Center alignment guide badge */}
          <div className="self-center bg-black/60 backdrop-blur-md px-3 py-1 rounded-full border border-white/20 text-[11px] font-semibold text-white/90">
            Center parking signs inside frame
          </div>

          <div className="flex justify-between">
            <div className="w-6 h-6 border-b-3 border-l-3 border-[#FFDAD1] rounded-bl-xl" />
            <div className="w-6 h-6 border-b-3 border-r-3 border-[#FFDAD1] rounded-br-xl" />
          </div>
        </div>

        {/* PROCESSING OVERLAY */}
        {isProcessing && (
          <div className="absolute inset-0 z-40 bg-black/70 backdrop-blur-xs flex flex-col items-center justify-center p-6 text-white text-center animate-in fade-in duration-200">
            <div className="w-16 h-16 rounded-full border-4 border-white/20 border-t-[#8F4C38] animate-spin mb-4" />
            <h3 className="font-extrabold text-lg text-white">Analyzing Parking Signs...</h3>
            <p className="text-xs text-white/80 max-w-xs mt-1">
              {processingStatusText || 'Isolating sign plates, checking current hour & schedule rules'}
            </p>
          </div>
        )}

        {/* SCAN ERROR BANNER */}
        {scanError && (
          <div className="absolute top-20 left-5 right-5 z-40 bg-[#FFDAD6] border border-[#BA1A1A] p-3.5 rounded-2xl shadow-lg flex items-center justify-between text-[#BA1A1A]">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <p className="text-xs font-bold leading-tight">{scanError}</p>
            </div>
            <button onClick={onClearError} className="p-1 hover:bg-black/5 rounded-lg">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>

      {/* 3. BOTTOM CONTROLS HUD */}
      <div className="relative z-30 pb-8 px-6 flex items-center justify-around">
        {/* Photo Gallery Button */}
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={isProcessing}
          className="w-14 h-14 rounded-full bg-white/20 backdrop-blur-md border border-white/30 text-white flex items-center justify-center hover:bg-white/30 active:scale-95 transition-all shadow-lg"
          title="Pick photo from gallery"
        >
          <ImageIcon className="w-6 h-6" />
        </button>

        {/* Primary Shutter Button */}
        <button
          onClick={handleShutter}
          disabled={isProcessing}
          className="w-20 h-20 rounded-full bg-white p-1.5 shadow-2xl active:scale-90 transition-transform flex items-center justify-center group"
          title="Capture sign photo"
        >
          <div className="w-full h-full rounded-full border-4 border-[#8F4C38] group-hover:bg-[#FFDAD1]/30 transition-colors flex items-center justify-center">
            <div className="w-12 h-12 rounded-full bg-[#8F4C38]" />
          </div>
        </button>

        {/* Flashlight Button */}
        <button
          onClick={() => setFlash(!flash)}
          disabled={isProcessing}
          className={`w-14 h-14 rounded-full backdrop-blur-md border text-white flex items-center justify-center active:scale-95 transition-all shadow-lg ${
            flash
              ? 'bg-[#FFDAD1] text-[#3A0B01] border-[#FFDAD1]'
              : 'bg-white/20 text-white border-white/30 hover:bg-white/30'
          }`}
          title="Toggle flash"
        >
          {flash ? <Zap className="w-6 h-6 fill-current" /> : <ZapOff className="w-6 h-6" />}
        </button>
      </div>

      {/* SAMPLE SIGNS DRAWER MODAL */}
      {showPresets && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-xs p-0 animate-in fade-in duration-200">
          <div className="w-full max-w-md bg-[#FFFFFF] rounded-t-3xl border border-[#EDE0DC] shadow-2xl p-6 max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-[#EDE0DC]">
              <div>
                <h3 className="font-black text-lg text-[#1F1B1A]">Sample Parking Signs</h3>
                <p className="text-xs text-[#85736E]">Test Curb without standing near physical signs</p>
              </div>
              <button
                onClick={() => setShowPresets(false)}
                className="w-8 h-8 rounded-full bg-[#F3E9E5] text-[#1F1B1A] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="mt-4 space-y-3">
              {presets.map((preset) => (
                <div
                  key={preset.id}
                  onClick={() => {
                    setShowPresets(false);
                    onSelectPreset(preset);
                  }}
                  className="p-4 rounded-2xl bg-[#FDF8F6] border border-[#EDE0DC] hover:border-[#8F4C38] cursor-pointer transition-all shadow-xs"
                >
                  <div className="flex items-center justify-between">
                    <h4 className="font-extrabold text-sm text-[#1F1B1A]">{preset.title}</h4>
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      preset.simulatedVerdict === 'ALLOWED'
                        ? 'bg-[#E8F5E9] text-[#2E7D32]'
                        : preset.simulatedVerdict === 'RESTRICTED'
                        ? 'bg-[#FFDAD6] text-[#BA1A1A]'
                        : 'bg-[#FFFFDCBE] text-[#9E4800]'
                    }`}>
                      {preset.simulatedVerdict}
                    </span>
                  </div>
                  <p className="text-xs text-[#85736E] mt-1">{preset.previewDescription}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* HELP MODAL */}
      {showHelp && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-xs p-0 animate-in fade-in duration-200">
          <div className="w-full max-w-md bg-[#FFFFFF] rounded-t-3xl border border-[#EDE0DC] shadow-2xl p-6">
            <div className="flex items-center justify-between pb-3 border-b border-[#EDE0DC]">
              <h3 className="font-black text-lg text-[#1F1B1A]">How Curb Works</h3>
              <button
                onClick={() => setShowHelp(false)}
                className="w-8 h-8 rounded-full bg-[#F3E9E5] text-[#1F1B1A] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="mt-4 space-y-4">
              <div className="flex gap-3">
                <div className="w-7 h-7 rounded-full bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center text-xs font-black flex-shrink-0">
                  1
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[#1F1B1A]">Target parking signs</h4>
                  <p className="text-xs text-[#85736E] mt-0.5">
                    Frame the signs on the post squarely. Curb highlights individual plates.
                  </p>
                </div>
              </div>

              <div className="flex gap-3">
                <div className="w-7 h-7 rounded-full bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center text-xs font-black flex-shrink-0">
                  2
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[#1F1B1A]">Read posted rules</h4>
                  <p className="text-xs text-[#85736E] mt-0.5">
                    Each sign is resolved against the current day, hour, and municipal code.
                  </p>
                </div>
              </div>

              <div className="flex gap-3">
                <div className="w-7 h-7 rounded-full bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center text-xs font-black flex-shrink-0">
                  3
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[#1F1B1A]">Get a clear verdict</h4>
                  <p className="text-xs text-[#85736E] mt-0.5">
                    Street sweeping, tow windows, and meter cutoffs merge into one safe decision.
                  </p>
                </div>
              </div>
            </div>

            <button
              onClick={() => setShowHelp(false)}
              className="w-full mt-6 py-3.5 rounded-2xl bg-[#8F4C38] text-white font-extrabold text-sm shadow-md hover:bg-[#3A0B01]"
            >
              Got it
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
