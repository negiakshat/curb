# Curb

**AI-powered parking assistant that scans parking signs with your camera and tells you exactly when you can park and for how long.**

Curb reads physical parking signs using your phone's camera, interprets complex rules (time limits, street cleaning, permit zones, meter requirements), and starts a verified countdown timer — so you never get a ticket from misunderstanding a sign.

## Features

- **AI Sign Scanning** — Point your camera at any parking sign. Curb uses Gemini Vision to read and interpret rules in real time.
- **Verified Parking Timer** — Timers are authority-bound: they respect the actual posted limits and cannot exceed them.
- **Smart Notifications** — Get push alerts before your session expires so you can move your car in time.
- **Find My Car** — Save your parking spot location and get walking directions back.
- **Saved Places** — Bookmark frequent parking spots with personal notes.
- **Scan History** — Browse all your past sign scans and results.
- **Ask Curb AI** — Chat with an AI assistant about parking rules for your current spot.
- **Curb Pro** — Unlock unlimited scans, exported reports, and notes via in-app subscription (RevenueCat).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Database | Room (local persistence) |
| AI / Vision | Google Gemini API (via Firebase AI), ML Kit Text Recognition |
| Camera | CameraX |
| Maps | OSMDroid |
| Location | Google Play Services Location |
| Networking | Retrofit + OkHttp + Moshi |
| Subscriptions | RevenueCat Purchases SDK |
| Auth / Backend | Firebase (Auth, Firestore, App Check) |
| Build | Gradle (AGP 9.1.1, Kotlin 2.2.10) |
| Testing | JUnit 4, Robolectric, Roborazzi (screenshot tests) |

## Setup

### Prerequisites

- **Android Studio** (latest stable, with AGP 9.1.1 support)
- **JDK 11+**
- **Android SDK** — compileSdk 36, minSdk 24

### 1. Clone the repository

```bash
git clone https://github.com/negiakshat/curb.git
cd curb
```

### 2. Configure API keys

Curb uses the [Secrets Gradle Plugin](https://github.com/google/secrets-gradle-plugin) to inject API keys from a `.env` file at the project root. Create `.env` from the example:

```bash
cp .env.example .env
```

Then fill in the required keys in `.env`:

| Key | Required | Description |
|-----|----------|-------------|
| `GEMINI_API_KEY` | Yes | Google AI Studio API key for Gemini Vision (sign scanning + chat) |
| `REVENUECAT_PUBLIC_API_KEY` | Yes | RevenueCat public API key for Curb Pro subscriptions |
| `FIREBASE_APPCHECK_DEBUG_TOKEN` | Optional | Firebase App Check debug token (for local dev) |

> **Do not commit `.env` to version control.** It is already in `.gitignore`.

You can obtain these keys from:
- **Gemini API Key** — [Google AI Studio](https://aistudio.google.com/apikey)
- **RevenueCat API Key** — [RevenueCat Dashboard](https://app.revenuecat.com) (create a project, add an Android app with package `com.aistudio.curb.xpyk`)

### 3. Build and run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

Or open the project in Android Studio and click **Run** on the `app` module.

### 4. Run tests

```bash
./gradlew testDebugUnitTest
```

## How It Works

1. **Scan** — Open the camera and point at a parking sign. Curb captures the image and sends it to Gemini Vision for analysis.
2. **Interpret** — The AI extracts the rule (e.g., "2 Hour Parking 8 AM–6 PM") and determines if parking is currently allowed.
3. **Timer** — If allowed, start a verified countdown timer. The timer is clamped to the actual posted limit and cannot be extended beyond it.
4. **Remind** — Curb sends a push notification before your time is up.

## Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Gemini API, RevenueCat, route service
│   ├── repository/     # CurbRepository (single data source)
│   ├── model/          # Data classes
│   └── detection/      # On-device sign detection
├── ui/
│   ├── screens/        # Compose screens
│   ├── components/     # Reusable UI components
│   ├── navigation/     # NavGraph and routes
│   └── theme/          # Material 3 theme
├── viewmodel/          # ViewModels and coordinators
├── notification/       # Push notification scheduling
└── util/               # Parking rule validation, timer math
```

## License

MIT — see [LICENSE](LICENSE).
