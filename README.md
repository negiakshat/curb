# Curb

**AI-powered parking assistant that scans parking signs with your camera, interprets municipal rules, and manages verified parking countdown timers.**

Curb reads physical parking signs using camera vision or image upload, interprets complex street rules (time limits, street sweeping, permit zones, meter cutoffs), and starts an authoritative countdown timer — so you never get a ticket from misunderstanding a sign.

## Features

- **AI Sign Scanning** — Point your camera at any parking sign or upload a photo. Curb isolates sign plates, runs OCR text analysis, and checks current hours against municipal regulations with Gemini Vision AI.
- **Sample Sign Presets** — Test the application instantly with pre-loaded presets: *2-Hour Metered Zone*, *Commute Tow-Away Zone*, and *Residential & Construction Area*.
- **Verified Parking Timer** — Live countdown timer bounded by physical signage authority. Displays active countdown progress ring, reminder notifications, and extension controls (+15m, +30m, +1h).
- **Find My Car** — Save your parking spot with GPS coordinates and view live walking routes, distance (feet/meters), walking time estimates, and step-by-step guidance.
- **Ask Curb Copilot** — Chat with an intelligent parking legal assistant. Ask questions like *"Can I park after 6 PM?"*, *"Why is this restricted?"*, or *"Are there permit exemptions?"*.
- **Saved Places** — Bookmark favorite parking spots with custom notes and re-check active sign restrictions with 1 tap.
- **Activity & Scan History** — Filter, search, and review all previous sign scans, verdict decisions, and rule breakdowns.
- **Curb Pro** — Unlock unlimited scans, citation defense evidence reports, and priority Copilot reasoning. Includes promo code redemption (e.g. `CURBPRO2026`).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19 + TypeScript + Vite |
| Styling | Tailwind CSS v4 + Bento Design System Palette |
| Icons | Lucide React |
| Server / API | Node.js + Express proxy + `@google/genai` |
| Vision AI | Google Gemini 2.5 Flash with municipal rule reasoning fallback |
| Persistence | Local Storage & Session State |

## Getting Started

### Prerequisites

- Node.js 22+
- npm

### Installation & Run

```bash
# Install dependencies
npm install

# Start development server on port 3000
npm run dev

# Build for production
npm run build
```

The app will be available on `http://localhost:3000`.

## License

MIT — see [LICENSE](LICENSE).
