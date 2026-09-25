import React, { useState } from 'react';
import { ArrowLeft, HelpCircle, Mail, MessageSquare, ExternalLink, ChevronDown, ChevronUp } from 'lucide-react';

interface HelpSupportScreenProps {
  onTerms: () => void;
  onPrivacy: () => void;
  onBack: () => void;
}

export const HelpSupportScreen: React.FC<HelpSupportScreenProps> = ({
  onTerms,
  onPrivacy,
  onBack
}) => {
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const faqs = [
    {
      q: 'How does Curb determine whether parking is allowed?',
      a: 'Curb uses computer vision to detect physical sign plates on the post, extracts hours, arrows, and day restrictions, then checks them against the active hour and local municipal code.'
    },
    {
      q: 'Can Curb defend me against an unfair ticket?',
      a: 'Yes. With Curb Pro, every scan creates an authoritative time-stamped proof record documenting visible signage and calculated permissions at the time you parked.'
    },
    {
      q: 'What if a sign is obstructed by trees or graffiti?',
      a: 'When signage is faded, obscured, or conflicting, Curb outputs an AMBIGUOUS (Rule Unclear) verdict to keep you safe and advises inspecting the physical post.'
    },
    {
      q: 'Do parking timers continue when the app is in the background?',
      a: 'Yes. Curb stores your parking countdown and sends notification reminders before meters or street sweeping windows expire.'
    }
  ];

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
          Help & Support
        </h2>

        <div className="w-11" />
      </div>

      <div className="p-5 space-y-4">
        {/* Contact Support Card */}
        <div className="bg-white border border-[#EDE0DC] rounded-3xl p-5 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-2xl bg-[#FFDAD1] text-[#8F4C38] flex items-center justify-center">
            <Mail className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-extrabold text-base text-[#1F1B1A]">
              Need Help With a Spot?
            </h3>
            <p className="text-xs text-[#85736E] mt-0.5">
              Our parking support engineers review municipal rules and edge cases.
            </p>
          </div>
          <a
            href="mailto:support@curbparking.app"
            className="w-full py-3 rounded-2xl bg-[#8F4C38] text-white text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm hover:bg-[#3A0B01]"
          >
            <Mail className="w-3.5 h-3.5" />
            <span>Email Support (support@curbparking.app)</span>
          </a>
        </div>

        {/* FAQs */}
        <div className="space-y-2">
          <h3 className="text-xs font-extrabold uppercase tracking-wider text-[#85736E] px-2">
            Frequently Asked Questions
          </h3>

          <div className="bg-white border border-[#EDE0DC] rounded-3xl divide-y divide-[#EDE0DC] shadow-xs overflow-hidden">
            {faqs.map((faq, i) => {
              const isOpen = openFaq === i;
              return (
                <div key={i} className="p-4">
                  <button
                    onClick={() => setOpenFaq(isOpen ? null : i)}
                    className="w-full flex items-center justify-between text-left text-xs font-bold text-[#1F1B1A]"
                  >
                    <span>{faq.q}</span>
                    {isOpen ? (
                      <ChevronUp className="w-4 h-4 text-[#85736E] flex-shrink-0 ml-2" />
                    ) : (
                      <ChevronDown className="w-4 h-4 text-[#85736E] flex-shrink-0 ml-2" />
                    )}
                  </button>
                  {isOpen && (
                    <p className="text-xs text-[#51433F] mt-2 leading-relaxed">
                      {faq.a}
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Legal Links */}
        <div className="pt-2 flex justify-center gap-4 text-xs font-semibold text-[#8F4C38]">
          <button onClick={onTerms} className="hover:underline">
            Terms of Service
          </button>
          <span>•</span>
          <button onClick={onPrivacy} className="hover:underline">
            Privacy Policy
          </button>
        </div>
      </div>
    </div>
  );
};
