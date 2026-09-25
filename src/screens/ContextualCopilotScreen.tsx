import React, { useState, useRef, useEffect } from 'react';
import {
  ArrowLeft,
  Send,
  Sparkles,
  Bot,
  User,
  ShieldCheck,
  Zap
} from 'lucide-react';
import { ChatMessage, ScanResult, ChatUsageInfo } from '../types';
import { CurbLogo } from '../components/CurbLogo';

interface ContextualCopilotScreenProps {
  messages: ChatMessage[];
  isLoading: boolean;
  scanResult: ScanResult | null;
  usageInfo: ChatUsageInfo;
  isPro: boolean;
  onSendMessage: (query: string) => void;
  onUpgradePro: () => void;
  onBack: () => void;
}

export const ContextualCopilotScreen: React.FC<ContextualCopilotScreenProps> = ({
  messages,
  isLoading,
  scanResult,
  usageInfo,
  isPro,
  onSendMessage,
  onUpgradePro,
  onBack
}) => {
  const [inputText, setInputText] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const handleSend = () => {
    if (!inputText.trim() || isLoading) return;
    onSendMessage(inputText.trim());
    setInputText('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // Suggested questions based on scan verdict
  const suggestions = (() => {
    if (!scanResult) {
      return [
        'Can I park at a green curb on Sunday?',
        'What does a red curb mean?',
        'How does street sweeping enforcement work?',
        'Are parking meters free on holidays?'
      ];
    }
    if (scanResult.verdict === 'RESTRICTED') {
      return [
        'Why is parking restricted right now?',
        'When CAN I safely park at this spot?',
        'Are there any permit exemptions?',
        'Which specific sign is restricting me?'
      ];
    }
    if (scanResult.verdict === 'ALLOWED') {
      return [
        'How long can I park here today?',
        'Do I need to pay a meter right now?',
        'What happens after 6 PM?',
        'When does street cleaning start?'
      ];
    }
    return [
      'What makes this sign ambiguous?',
      'How do I verify the rule on-site?',
      'Is payment required at this curb?',
      'What do the directional arrows mean?'
    ];
  })();

  return (
    <div className="min-h-screen bg-[#FDF8F6] flex flex-col justify-between max-w-md mx-auto">
      {/* 1. TOP APP BAR */}
      <div className="sticky top-0 z-30 bg-[#FDF8F6]/95 backdrop-blur-md px-5 pt-4 pb-3 border-b border-[#EDE0DC]">
        <div className="flex items-center justify-between">
          <button
            onClick={onBack}
            className="w-11 h-11 rounded-full bg-white border border-[#EDE0DC] text-[#1F1B1A] flex items-center justify-center hover:bg-[#F3E9E5] transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-extrabold text-[#1F1B1A]">
                Ask Curb Copilot
              </h2>
              <p className="text-[10px] text-[#85736E] font-medium">
                {isPro ? 'Pro • Unlimited Q&A' : `${Math.max(0, usageInfo.maxFreeMessages - usageInfo.messagesUsedToday)} free questions left today`}
              </p>
            </div>
          </div>

          <div className="w-11" />
        </div>

        {/* Scan Context Badge if active */}
        {scanResult && (
          <div className="mt-2.5 px-3 py-1.5 rounded-2xl bg-[#FFFFFF] border border-[#EDE0DC] flex items-center justify-between text-xs">
            <span className="text-[#85736E] truncate">
              Spot: <strong className="text-[#1F1B1A]">{scanResult.locationName}</strong>
            </span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
              scanResult.verdict === 'ALLOWED'
                ? 'bg-[#E8F5E9] text-[#2E7D32]'
                : scanResult.verdict === 'RESTRICTED'
                ? 'bg-[#FFDAD6] text-[#BA1A1A]'
                : 'bg-[#FFFFDCBE] text-[#9E4800]'
            }`}>
              {scanResult.verdict}
            </span>
          </div>
        )}
      </div>

      {/* 2. CHAT MESSAGES SCROLL AREA */}
      <div className="flex-1 overflow-y-auto p-5 space-y-4">
        {messages.length === 0 ? (
          <div className="py-8 text-center space-y-4">
            <div className="w-14 h-14 rounded-3xl bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center mx-auto shadow-xs">
              <Sparkles className="w-7 h-7" />
            </div>

            <div>
              <h3 className="font-extrabold text-base text-[#1F1B1A]">
                {scanResult ? `Ask about ${scanResult.locationName}` : 'Ask Anything About Parking'}
              </h3>
              <p className="text-xs text-[#85736E] max-w-xs mx-auto mt-1">
                Got questions about meter cutoffs, weekend rules, permit zones, or street cleaning? Curb has you covered.
              </p>
            </div>

            {/* Quick Suggestions Chips */}
            <div className="pt-2 flex flex-col gap-2 max-w-xs mx-auto text-left">
              {suggestions.map((q, idx) => (
                <button
                  key={idx}
                  onClick={() => onSendMessage(q)}
                  className="p-2.5 rounded-2xl bg-white border border-[#EDE0DC] hover:border-[#8F4C38] text-xs font-semibold text-[#1F1B1A] transition-colors shadow-2xs text-left"
                >
                  &ldquo;{q}&rdquo;
                </button>
              ))}
            </div>
          </div>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`flex items-start gap-2.5 ${msg.isUser ? 'flex-row-reverse' : ''}`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 ${
                  msg.isUser
                    ? 'bg-[#8F4C38] text-white'
                    : 'bg-[#FFDAD1] text-[#8F4C38]'
                }`}
              >
                {msg.isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
              </div>

              <div
                className={`max-w-[80%] rounded-3xl p-3.5 text-xs font-medium leading-relaxed whitespace-pre-line shadow-xs ${
                  msg.isUser
                    ? 'bg-[#3A0B01] text-white rounded-tr-xs'
                    : 'bg-[#FFFFFF] text-[#1F1B1A] border border-[#EDE0DC] rounded-tl-xs'
                }`}
              >
                {msg.text}
              </div>
            </div>
          ))
        )}

        {isLoading && (
          <div className="flex items-center gap-2 text-xs text-[#85736E] py-2">
            <div className="w-2 h-2 rounded-full bg-[#8F4C38] animate-bounce" />
            <div className="w-2 h-2 rounded-full bg-[#8F4C38] animate-bounce [animation-delay:0.2s]" />
            <div className="w-2 h-2 rounded-full bg-[#8F4C38] animate-bounce [animation-delay:0.4s]" />
            <span className="font-semibold text-[11px] ml-1">Curb Copilot is analyzing rules...</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* 3. INPUT BAR */}
      <div className="sticky bottom-0 z-30 bg-[#FDF8F6] p-4 border-t border-[#EDE0DC]">
        {/* Suggestion Quick Chips if some messages already exist */}
        {messages.length > 0 && messages.length < 5 && (
          <div className="flex gap-2 overflow-x-auto pb-2 mb-1 scrollbar-none">
            {suggestions.slice(0, 3).map((q, i) => (
              <button
                key={i}
                onClick={() => onSendMessage(q)}
                className="whitespace-nowrap px-3 py-1 rounded-full bg-white border border-[#EDE0DC] text-[11px] font-semibold text-[#8F4C38] hover:bg-[#F3E9E5] transition-colors"
              >
                {q}
              </button>
            ))}
          </div>
        )}

        <div className="flex items-center gap-2">
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={scanResult ? `Ask about this sign...` : `Ask any parking question...`}
            className="flex-1 text-xs font-medium p-3.5 rounded-2xl border border-[#D6C2BC] bg-[#FFFFFF] text-[#1F1B1A] focus:outline-none focus:border-[#8F4C38] shadow-xs"
          />

          <button
            onClick={handleSend}
            disabled={!inputText.trim() || isLoading}
            className="w-12 h-12 rounded-2xl bg-[#8F4C38] text-white flex items-center justify-center disabled:opacity-40 hover:bg-[#3A0B01] active:scale-95 transition-all shadow-md shadow-[#8F4C38]/20 flex-shrink-0"
          >
            <Send className="w-5 h-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
