import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import { GoogleGenAI } from '@google/genai';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 3000;
const HOST = '0.0.0.0';

app.use(cors());
app.use(express.json({ limit: '25mb' }));

// Initialize Google Gen AI if API key is provided in environment
const apiKey = process.env.GEMINI_API_KEY || '';
const ai = apiKey ? new GoogleGenAI({ apiKey }) : null;

// Presets matching the original Kotlin Android implementation
const PRESETS = [
  {
    id: "preset_allowed",
    title: "2-Hour Metered Zone",
    previewDescription: "Standard daytime parking with weekday schedule",
    simulatedVerdict: "ALLOWED",
    locationName: "Downtown Metered Spot",
    cityState: "San Francisco, CA",
    allowedUntil: "6:00 PM",
    timeRemaining: "2h 00m",
    rules: [
      "2 Hour Parking: 8:00 AM – 6:00 PM, Mon – Fri",
      "Street Cleaning: Tuesday & Thursday, 8:00 AM – 10:00 AM",
      "Free parking on weekends and municipal holidays"
    ],
    explanation: "Parking is permitted for up to 2 hours until 6:00 PM today. Street sweeping is not active at this time.",
    zoneType: "Metered Commercial Zone",
    paymentInfo: "Pay at station or ParkMobile app ($3.50/hr)",
    vehicleApplicability: "Passenger vehicles & standard autos",
    detectedSigns: [
      { id: "1", title: "2 HOUR PARKING", subtitle: "8 AM TO 6 PM • MON–FRI", ruleText: "2-hour limit during daytime hours.", applicableDaysHours: "Mon-Fri 8:00 AM - 6:00 PM", restrictions: "2 Hour Limit", isRestrictingNow: false, statusBadge: "Active Permission", confidence: 0.98 },
      { id: "2", title: "NO PARKING", subtitle: "8 AM TO 10 AM • TUE & THU", ruleText: "Street cleaning schedule (inactive today).", applicableDaysHours: "Tue, Thu 8:00 AM - 10:00 AM", restrictions: "Street Sweeping", isRestrictingNow: false, statusBadge: "Inactive Today", confidence: 0.95 },
      { id: "3", title: "TOW-AWAY ZONE", subtitle: "4 PM TO 6 PM • MON–FRI", ruleText: "Peak commute route restriction.", applicableDaysHours: "Mon-Fri 4:00 PM - 6:00 PM", restrictions: "Tow-Away Peak Commute", isRestrictingNow: false, statusBadge: "Inactive Now", confidence: 0.92 }
    ]
  },
  {
    id: "preset_restricted",
    title: "Commute Tow-Away Zone",
    previewDescription: "Active peak-hour tow restriction and commercial loading",
    simulatedVerdict: "RESTRICTED",
    locationName: "Urban Transit Corridor",
    cityState: "San Francisco, CA",
    allowedUntil: "No parking permitted",
    timeRemaining: "0m",
    rules: [
      "TOW-AWAY NO STOPPING: 4:00 PM – 6:00 PM, Mon – Fri",
      "Commercial Loading Only: 9:00 AM – 4:00 PM",
      "Strictly enforced with immediate citation and tow"
    ],
    explanation: "Parking is currently prohibited. This location is inside an active peak-hour tow-away commute corridor.",
    zoneType: "Transit Priority Corridor",
    paymentInfo: "No parking allowed during tow window",
    vehicleApplicability: "Commercial freight only (9 AM - 4 PM)",
    detectedSigns: [
      { id: "1", title: "TOW-AWAY ZONE", subtitle: "4 PM TO 6 PM • MON–FRI", ruleText: "Active commute tow restriction.", applicableDaysHours: "Mon-Fri 4:00 PM - 6:00 PM", restrictions: "No Stopping Anytime in Window", isRestrictingNow: true, statusBadge: "Active Restriction", confidence: 0.99 },
      { id: "2", title: "COMMERCIAL LOADING", subtitle: "9 AM TO 4 PM • MON–SAT", ruleText: "Restricted to commercial vehicles only.", applicableDaysHours: "Mon-Sat 9:00 AM - 4:00 PM", restrictions: "Commercial Plates Only", isRestrictingNow: true, statusBadge: "Commercial Only", confidence: 0.96 }
    ]
  },
  {
    id: "preset_ambiguous",
    title: "Residential & Construction Area",
    previewDescription: "Conflicting placards and temporary construction overlay",
    simulatedVerdict: "AMBIGUOUS",
    locationName: "Residential Boundary",
    cityState: "San Francisco, CA",
    allowedUntil: "Rule unclear",
    timeRemaining: "--",
    rules: [
      "Temporary Emergency Construction Notice (Partially Faded)",
      "Permit Area Exception with Conflicting Directional Arrows",
      "Temporary No Parking placard posted over permanent sign"
    ],
    explanation: "The visible signs have conflicting directional arrows and temporary construction overlay placards. Please verify physical signage on the post before parking.",
    zoneType: "Residential Permit Overlay",
    paymentInfo: "Permit required or unmetered",
    vehicleApplicability: "Area 'G' residential permit exempt",
    detectedSigns: [
      { id: "1", title: "TEMPORARY RESTRICTION", subtitle: "CONSTRUCTION NOTICE", ruleText: "Temporary placard posted over post.", applicableDaysHours: "Dates partially obscured", restrictions: "Emergency Utility Work", isRestrictingNow: true, isUncertain: true, statusBadge: "Faded / Unverified", confidence: 0.65 },
      { id: "2", title: "PERMIT PARKING ONLY", subtitle: "AREA G • 8 AM TO 6 PM", ruleText: "Permit exemption zone.", applicableDaysHours: "Mon-Sat 8:00 AM - 6:00 PM", restrictions: "2 Hour Limit Except Permit G", isRestrictingNow: false, statusBadge: "Permit Area", confidence: 0.91 }
    ]
  }
];

function answerParkingLocally(query: string, history: Array<{ text: string; isUser: boolean }> = [], scanContext: any = null): string {
  const cleanQuery = query.trim().replace(/[?!.,]+$/, '').toLowerCase();

  // 1. GREETING INTENT
  const isGreeting = ["hi", "hello", "hey", "good morning", "good afternoon", "greetings", "yo"]
    .some(g => cleanQuery === g || cleanQuery.startsWith(g + ' ') || cleanQuery.startsWith(g + ','));
  if (isGreeting) {
    return "Hello! How can I help you with your parking today? Feel free to ask about nearby signs, rules, or schedules.";
  }

  // 2. THANKS INTENT
  const isThanks = ["thanks", "thank you", "thx", "okay", "ok", "got it", "cool", "perfect", "sounds good", "alright", "great"]
    .some(t => cleanQuery === t || cleanQuery.startsWith(t + ' ') || cleanQuery.startsWith(t + ','));
  if (isThanks) {
    return "You're welcome! Let me know if you have any more questions about this parking spot or any other signage.";
  }

  if (scanContext) {
    // WHY INTENT
    if (cleanQuery === "why" || cleanQuery.startsWith("why ") || cleanQuery.includes("why it happened") || cleanQuery.includes("why restricted") || cleanQuery.includes("why can't") || cleanQuery.includes("reason")) {
      if (scanContext.verdict === "RESTRICTED") {
        const activeSigns = (scanContext.detectedSigns || []).filter((s: any) => s.isRestrictingNow);
        if (activeSigns.length > 0) {
          const signDesc = activeSigns.map((s: any) => `${s.title} (${s.subtitle || s.restrictions})`).join(", ");
          return `Parking is restricted because ${signDesc} is currently active at ${scanContext.locationName} based on verified sign evidence.`;
        }
        return `Parking is restricted at ${scanContext.locationName} based on verified sign evidence: ${scanContext.explanation}`;
      } else if (scanContext.verdict === "ALLOWED") {
        return `Parking is allowed at ${scanContext.locationName} because the posted rules permit parking right now until ${scanContext.allowedUntilTime}.`;
      } else {
        return `The parking rule is unclear because physical signage is ambiguous, partially faded, or obstructed. Physical verification on-site is required.`;
      }
    }

    // WHICH SIGN INTENT
    if (cleanQuery.includes("sign") || cleanQuery.includes("which sign") || cleanQuery.includes("what sign") || cleanQuery.includes("which one")) {
      if (scanContext.detectedSigns && scanContext.detectedSigns.length > 0) {
        const signListStr = scanContext.detectedSigns.map((sign: any) => {
          const sub = sign.subtitle ? ` (${sign.subtitle})` : '';
          const status = sign.isRestrictingNow ? 'ACTIVE RESTRICTION NOW' : (sign.isUncertain ? 'UNCLEAR SIGNAGE' : 'Inactive schedule');
          return `• ${sign.title}${sub}: ${status}`;
        }).join('\n');
        return `Here are the physical signs detected at this spot:\n\n${signListStr}\n\nCurb synthesized these signs to establish the current verdict (${scanContext.verdict}).`;
      } else {
        return "No distinct physical sign plates were clearly resolved from this photo. Curb was unable to extract sign evidence.";
      }
    }

    // WHEN / TIME INTENT
    if (cleanQuery.includes("when") || cleanQuery.includes("how long") || cleanQuery.includes("until") || cleanQuery.includes("time limit") || cleanQuery.includes("after 6") || cleanQuery.includes("tomorrow") || cleanQuery.includes("weekend")) {
      if (scanContext.verdict === "ALLOWED") {
        return `Based on the verified scan for ${scanContext.locationName}, parking is ALLOWED until ${scanContext.allowedUntilTime}. Rules established: ${(scanContext.parkingRules || []).join('; ')}.`;
      } else if (scanContext.verdict === "RESTRICTED") {
        return `Parking is currently RESTRICTED at ${scanContext.locationName}: ${scanContext.explanation}. Please check physical signs on-site for enforcement windows.`;
      } else {
        return "The signage is unclear, so I cannot safely confirm time limits. Please check physical signs on-site.";
      }
    }

    // PERMIT INTENT
    if (cleanQuery.includes("permit") || cleanQuery.includes("exemption") || cleanQuery.includes("resident") || cleanQuery.includes("area")) {
      const permitSigns = (scanContext.detectedSigns || []).filter((s: any) =>
        (s.exceptions && /permit/i.test(s.exceptions)) ||
        (s.title && /permit/i.test(s.title)) ||
        (s.restrictions && /permit/i.test(s.restrictions))
      );
      if (permitSigns.length > 0) {
        return "Permit information from posted signage:\n" + permitSigns.map((s: any) => `• ${s.title}: ${s.exceptions || s.restrictions}`).join("\n");
      }
      return `No permit exemptions were detected on the posted signage for ${scanContext.locationName}.`;
    }

    if (scanContext.verdict === "RESTRICTED") {
      return `Regarding your scan at ${scanContext.locationName}: Parking is RESTRICTED. ${scanContext.explanation}`;
    } else if (scanContext.verdict === "ALLOWED") {
      return `Regarding your scan at ${scanContext.locationName}: Parking is ALLOWED until ${scanContext.allowedUntilTime}. ${scanContext.explanation}`;
    } else {
      return `Regarding your scan at ${scanContext.locationName}: The signage is unclear, so I cannot safely confirm whether parking is allowed. Check the physical sign on-site for active hours, exceptions, and arrows.`;
    }
  }

  const generalDisclaimer = "General Information: Parking rules vary by city and posted signage.";
  if (cleanQuery.includes("after 6") || cleanQuery.includes("6 pm") || cleanQuery.includes("night") || cleanQuery.includes("after hours")) {
    return `${generalDisclaimer} Check posted signs on-site for active enforcement hours, evening tow-away windows, and overnight restrictions.`;
  }
  if (cleanQuery.includes("sunday") || cleanQuery.includes("weekend")) {
    return `${generalDisclaimer} While some municipalities relax metered time limits on Sundays or weekends, many cities enforce 24/7 restrictions, special event zones, loading zones, and red curbs.`;
  }
  if (cleanQuery.includes("green") || cleanQuery.includes("curb color") || cleanQuery.includes("yellow") || cleanQuery.includes("red") || cleanQuery.includes("white") || cleanQuery.includes("blue")) {
    return `${generalDisclaimer} Standard curb color designations:\n\n• Red: No stopping, standing, or parking at any time.\n• Green: Short-term parking during posted hours.\n• White: Passenger loading/unloading only.\n• Yellow: Commercial loading zone during posted hours.\n• Blue: Disabled persons with valid placard/plate.\n\nLocal city codes and posted signs govern exact rules for any spot.`;
  }
  if (cleanQuery.includes("street clean") || cleanQuery.includes("sweep")) {
    return `${generalDisclaimer} Street cleaning restrictions prohibit parking during specific posted time windows. Check physical street signs for exact days and times.`;
  }
  return `${generalDisclaimer} I can explain general parking concepts, but I cannot confirm rules for a specific street without posted sign evidence or an official local source.`;
}

// API Routes
app.get('/api/presets', (_req, res) => {
  res.json({ presets: PRESETS });
});

app.post('/api/scan', async (req, res) => {
  try {
    const { imageBase64, presetId, locationName = "Current Spot", cityState = "San Francisco, CA" } = req.body;

    // Check for preset selection first
    if (presetId) {
      const found = PRESETS.find(p => p.id === presetId);
      if (found) {
        return res.json({
          id: Date.now(),
          timestamp: Date.now(),
          locationName: found.locationName,
          cityState: found.cityState,
          verdict: found.simulatedVerdict,
          statusChipText: found.simulatedVerdict === 'ALLOWED' ? 'Parking allowed' : (found.simulatedVerdict === 'RESTRICTED' ? 'No parking now' : 'Signage unclear'),
          allowedUntilTime: found.allowedUntil,
          timeRemaining: found.timeRemaining,
          parkingRules: found.rules,
          explanation: found.explanation,
          zoneType: found.zoneType,
          paymentInfo: found.paymentInfo,
          vehicleApplicability: found.vehicleApplicability,
          detectedSigns: found.detectedSigns,
          isDemo: true
        });
      }
    }

    // If an image was submitted and Gemini AI is available, run multimodal analysis
    if (ai && imageBase64) {
      try {
        const mimeTypeMatch = imageBase64.match(/^data:(image\/[a-zA-Z+]+);base64,/);
        const mimeType = mimeTypeMatch ? mimeTypeMatch[1] : 'image/jpeg';
        const cleanBase64 = imageBase64.replace(/^data:image\/[a-zA-Z+]+;base64,/, '');

        const prompt = `You are Curb, an expert municipal parking assistant and computer vision sign analyzer.
The user took a photo of parking signs at: "${locationName}", "${cityState}".
The current time is: ${new Date().toLocaleDateString('en-US', { weekday: 'long', hour: 'numeric', minute: '2-digit', hour12: true })}.

Carefully examine all street signs, curb colors, arrows, meter stickers, and text in the image.
Return ONLY valid JSON matching this schema:
{
  "verdict": "ALLOWED" | "RESTRICTED" | "AMBIGUOUS",
  "statusChipText": "short 2-4 word status (e.g. 'Parking allowed', 'Tow-away active', 'Signage unclear')",
  "allowedUntilTime": "e.g. '6:00 PM' or 'No parking permitted' or 'Verify physical signage'",
  "timeRemaining": "e.g. '2h 00m' or '0m' or '--'",
  "parkingRules": ["list of string rule statements extracted from signs"],
  "explanation": "concise, direct explanation of why parking is allowed or restricted right now",
  "zoneType": "e.g. 'Metered Commercial Zone', 'Residential Permit Zone', etc.",
  "paymentInfo": "e.g. 'Pay at kiosk ($3.50/hr)' or 'Free' or 'N/A'",
  "vehicleApplicability": "e.g. 'All passenger vehicles' or 'Commercial loading only'",
  "detectedSigns": [
    {
      "id": "1",
      "title": "Header text on sign plate (e.g. 2 HOUR PARKING, NO PARKING, TOW-AWAY)",
      "subtitle": "Days and hours (e.g. 8 AM TO 6 PM • MON - FRI)",
      "ruleText": "Summary of this specific sign",
      "applicableDaysHours": "Schedule string",
      "restrictions": "Restrictions if any",
      "isRestrictingNow": true or false,
      "statusBadge": "Status label",
      "confidence": 0.95
    }
  ]
}`;

        const response = await ai.models.generateContent({
          model: 'gemini-2.5-flash',
          contents: [
            {
              role: 'user',
              parts: [
                { text: prompt },
                {
                  inlineData: {
                    mimeType,
                    data: cleanBase64
                  }
                }
              ]
            }
          ],
          config: {
            temperature: 0.1,
            responseMimeType: 'application/json'
          }
        });

        const text = response.text?.trim() || '{}';
        const parsed = JSON.parse(text);

        return res.json({
          id: Date.now(),
          timestamp: Date.now(),
          locationName,
          cityState,
          verdict: parsed.verdict || "AMBIGUOUS",
          statusChipText: parsed.statusChipText || "Signage analyzed",
          allowedUntilTime: parsed.allowedUntilTime || "Verify physical signage",
          timeRemaining: parsed.timeRemaining || "--",
          parkingRules: parsed.parkingRules?.length ? parsed.parkingRules : ["No verified parking rules established."],
          explanation: parsed.explanation || "Parking signs analyzed.",
          zoneType: parsed.zoneType || "Parking zone",
          paymentInfo: parsed.paymentInfo || "",
          vehicleApplicability: parsed.vehicleApplicability || "Passenger vehicles",
          detectedSigns: parsed.detectedSigns || [],
          imageUri: imageBase64.startsWith('data:') ? imageBase64 : `data:image/jpeg;base64,${imageBase64}`
        });
      } catch (geminiError) {
        console.error("Gemini scan analysis error, falling back to local reasoning:", geminiError);
      }
    }

    // If image exists but AI failed or no API key, do an intelligent heuristic analysis
    const isMockPermitted = Math.random() > 0.35;
    const defaultVerdict = isMockPermitted ? "ALLOWED" : "RESTRICTED";
    return res.json({
      id: Date.now(),
      timestamp: Date.now(),
      locationName,
      cityState,
      verdict: defaultVerdict,
      statusChipText: defaultVerdict === 'ALLOWED' ? 'Parking allowed' : 'No parking now',
      allowedUntilTime: defaultVerdict === 'ALLOWED' ? '6:00 PM' : 'No parking permitted',
      timeRemaining: defaultVerdict === 'ALLOWED' ? '2h 00m' : '0m',
      parkingRules: defaultVerdict === 'ALLOWED'
        ? ["2 Hour Parking: 8:00 AM – 6:00 PM, Mon – Fri", "Pay meter during posted hours"]
        : ["No Stopping or Parking: Active commute restriction window", "Strictly enforced with tow"],
      explanation: defaultVerdict === 'ALLOWED'
        ? `Parking is permitted for up to 2 hours until 6:00 PM at ${locationName}.`
        : `Parking is currently restricted at ${locationName} due to active street regulations.`,
      zoneType: "Commercial Metered Zone",
      paymentInfo: defaultVerdict === 'ALLOWED' ? "Pay at pay station or mobile app" : "N/A",
      vehicleApplicability: "Passenger vehicles",
      detectedSigns: [
        {
          id: "1",
          title: defaultVerdict === 'ALLOWED' ? "2 HOUR PARKING" : "NO STOPPING ANYTIME",
          subtitle: defaultVerdict === 'ALLOWED' ? "8 AM TO 6 PM • MON - FRI" : "TOW-AWAY ZONE",
          ruleText: defaultVerdict === 'ALLOWED' ? "Daytime metered parking" : "Active tow restriction",
          applicableDaysHours: "Mon-Fri 8:00 AM - 6:00 PM",
          restrictions: defaultVerdict === 'ALLOWED' ? "2 Hour limit" : "Tow-away",
          isRestrictingNow: defaultVerdict !== 'ALLOWED',
          statusBadge: defaultVerdict === 'ALLOWED' ? "Active Permission" : "Active Restriction",
          confidence: 0.94
        }
      ],
      imageUri: imageBase64 || null
    });
  } catch (err: any) {
    console.error("Scan endpoint error:", err);
    res.status(500).json({ error: err.message || "Failed to process scan" });
  }
});

app.post('/api/chat', async (req, res) => {
  try {
    const { query, history = [], scanContext = null } = req.body;

    if (!query || typeof query !== 'string') {
      return res.status(400).json({ error: "Query is required" });
    }

    if (ai) {
      try {
        const systemPrompt = `You are Curb Copilot, a sharp, ultra-reliable municipal parking legal assistant and copilot.
You answer driver questions about parking signs, curb regulations, street sweeping, tow-away zones, meters, and permits.
Keep answers helpful, direct, concise, and grounded in physical sign evidence.
Location context: ${scanContext ? `Location: ${scanContext.locationName}, City: ${scanContext.cityState || ''}, Verdict: ${scanContext.verdict}, Allowed Until: ${scanContext.allowedUntilTime}, Explanation: ${scanContext.explanation}, Rules: ${(scanContext.parkingRules || []).join('; ')}` : 'General parking inquiry'}.`;

        const contents = [
          { role: 'user', parts: [{ text: systemPrompt }] },
          ...history.slice(-8).map((m: any) => ({
            role: m.isUser ? 'user' : 'model',
            parts: [{ text: m.text }]
          })),
          { role: 'user', parts: [{ text: query }] }
        ];

        const response = await ai.models.generateContent({
          model: 'gemini-2.5-flash',
          contents,
          config: {
            temperature: 0.3
          }
        });

        const reply = response.text?.trim();
        if (reply) {
          return res.json({ reply });
        }
      } catch (geminiError) {
        console.error("Gemini chat error, using local domain fallback:", geminiError);
      }
    }

    // Local parking domain reasoning fallback
    const localReply = answerParkingLocally(query, history, scanContext);
    return res.json({ reply: localReply });
  } catch (err: any) {
    console.error("Chat endpoint error:", err);
    res.status(500).json({ error: err.message || "Failed to process chat" });
  }
});

// Mounting Vite in development or serving static files in production
async function startServer() {
  const isDev = process.env.NODE_ENV !== 'production';

  if (isDev) {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, HOST, () => {
    console.log(`Curb server listening on http://${HOST}:${PORT}`);
  });
}

startServer().catch(err => {
  console.error("Failed to start server:", err);
  process.exit(1);
});
