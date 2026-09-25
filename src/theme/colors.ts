// Bento Grid Design System Palette (Warm Terracotta, Espresso, Sand & Cream)
export const BentoCanvas = '#FDF8F6';          // Warm creamy off-white background
export const BentoPrimary = '#8F4C38';         // Rich terracotta / rust clay accent
export const BentoPrimaryDark = '#3A0B01';     // Deep roasted espresso umber
export const BentoPeach = '#FFDAD1';           // Soft warm peach hero tint
export const BentoSand = '#F3E9E5';            // Warm sand card container
export const BentoBeige = '#F9EEE8';           // Warm light biscuit container
export const BentoTextPrimary = '#1F1B1A';     // Deep charcoal-umber text
export const BentoTextSecondary = '#85736E';   // Warm muted brown-gray
export const BentoTextDark = '#51433F';        // Mid brown-charcoal
export const BentoBorder = '#EDE0DC';          // Subtle warm divider/border
export const BentoBorderStrong = '#D6C2BC';    // Structured border / chart bar
export const BentoWhite = '#FFFFFF';           // Pure white elements & cards

// Semantic Status Badges
export const CurbSuccess = '#2E7D32';
export const CurbSuccessContainer = '#E8F5E9';

export const CurbError = '#BA1A1A';
export const CurbErrorContainer = '#FFDAD6';

export const CurbWarning = '#9E4800';
export const CurbWarningContainer = '#FFDCBE';

// Helper function to return styling classes for verdicts
export function getVerdictStyles(verdict: 'ALLOWED' | 'RESTRICTED' | 'AMBIGUOUS') {
  switch (verdict) {
    case 'ALLOWED':
      return {
        bg: 'bg-[#E8F5E9]',
        text: 'text-[#2E7D32]',
        border: 'border-[#2E7D32]/30',
        badge: 'bg-[#2E7D32] text-white',
        title: 'Parking allowed',
        subtitle: 'You can park here under the current rules.'
      };
    case 'RESTRICTED':
      return {
        bg: 'bg-[#FFDAD6]',
        text: 'text-[#BA1A1A]',
        border: 'border-[#BA1A1A]/30',
        badge: 'bg-[#BA1A1A] text-white',
        title: 'Parking restricted',
        subtitle: 'An active rule prohibits parking at this spot right now.'
      };
    case 'AMBIGUOUS':
    default:
      return {
        bg: 'bg-[#FFFFDCBE]',
        text: 'text-[#9E4800]',
        border: 'border-[#9E4800]/30',
        badge: 'bg-[#9E4800] text-white',
        title: 'Rule unclear',
        subtitle: 'Curb could not confidently determine the active parking rule.'
      };
  }
}
