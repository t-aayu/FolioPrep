# FolioPrep — UPSC CSE Study Studio

**FolioPrep** is a local-first Android study studio and offline quiz builder built exclusively for **UPSC Civil Services Examination (CSE)** aspirants.

Extract clean text locally from articles, textbook photos, or personal notes; craft exam-calibrated prompts as a senior UPSC CSE Question Setter; send them directly to your installed AI apps (**ChatGPT**, **Gemini**, **Claude**, **Perplexity**, or any Android share target); and auto-import the structured JSON reply into an offline **Room** library for **Prelims Practice**, **Timed Prelims Simulation**, and **Mains 10/15-Mark Flashcards**.

---

## Highlights

- **100% Local Pre-Processing**
  - **Direct Text**: Paste editorials, PIB briefs, or notes directly with automatic ~2,500-word chunking for long sources.
  - **Article Link Extractor**: Fetches web pages via OkHttp and strips navigation, ads, and sidebars locally using Jsoup.
  - **On-Device OCR**: Pick multiple pages via the zero-permission Android Photo Picker or capture pages with the camera using bundled Google ML Kit Text Recognition.
  - **Smart Clipboard Auto-Paste**: Automatically fills empty Article URL or Source Text boxes when switching tabs or returning to the app.

- **Persistent UPSC CSE Exam Pattern Calibration**
  - Pin your **Latest Prelims Year** and **Latest Mains Year** right at the top of the Library homepage (persisted permanently across restarts and updates via DataStore).
  - Prompts automatically adapt to reflect the official UPSC CSE Prelims and Mains pattern of your chosen year and all previous years.

- **UPSC CSE Question Setter Prompt Studio**
  - Defaults to a senior **UPSC CSE Question Setter** (seasoned university professor) persona, with a Settings toggle to switch to your own saved **Custom Skill**.
  - Correlates current-affairs source material with static UPSC CSE syllabus knowledge and Previous Year Question (PYQ) trends.
  - Mode-specific subject tags:
    - **Prelims MCQ**: Polity, Economy, Environment, History, Geography, Sci-Tech, IR, Government Schemes, Current Affairs
    - **Mains 10/15M**: Geography, Indian History, World History, Indian Society, Polity and Governance, IR, Social Justice, Economy, Internal Security, Sci-Tech, Environment Biodiversity, Disaster Management, Ethics integrity and aptitude
    - **Both**: History, Geography, Polity, IR, Economy, Sci-tech, Environment
  - Full in-app Prompt Editor with monospace formatting and pretty-printed ` ```json ` schema output.

- **Seamless External AI Handoff & Resilient Auto-Import**
  - Highlighted **ChatGPT →** and **Gemini →** quick-send pills, plus one-tap Android Sharesheet and Copy Prompt support.
  - Automatic clipboard detection when returning to FolioPrep (`onWindowFocusChanged`).
  - Resilient JSON parser that handles fenced code snippets, pretty-printed indentation, multi-line UPSC statement stems (`\n` / `\n\n`), markdown-escaped punctuation (`\[ \] \{ \} \& \- \_`), and trailing commas.
  - Interactive **Verify & Save Deck** preview screen to edit titles, tags, MCQs, or Mains blueprints before saving offline.

- **Offline Revision Modes (Room Database)**
  - **Prelims Practice Mode**: Untimed mode with instant answer reveal, UPSC Question Setter explanations, and score history.
  - **Timed Prelims Exam Mode**: Live countdown timer, jumpable question palette, and configurable negative marking (including official UPSC `-1/3`).
  - **Mains 10/15-Mark Flashcards**: Spring-animated 3D flip cards revealing structured **Introduction → Core Arguments → Conclusion / Way Forward** blueprints with mastery tracking.

- **Editorial Design & Settings**
  - **Warm Editorial Paper** (Light), **Warm Ink** (Dark), and **System** theme switching in Settings.
  - Bundled typography (`Newsreader`, `Plus Jakarta Sans`, `JetBrains Mono`) and adaptive Navigation Bar / Navigation Rail support for phones, foldables, and tablets.

---

## How the 4-Stage Workflow Works

```text
[1. Local Extract]  ──▶  [2. Prompt Studio]  ──▶  [3. External AI]  ──▶  [4. Auto-Import & Study]
 Text / URL / OCR        Configure counts,        Tap ChatGPT →,        Copy JSON reply & return;
 cleaned on-device       mode, tags & skill       Gemini →, or Share    verify & save to Room DB
```

1. **Extract**: Paste text, extract an article link with Jsoup, or scan notes with on-device ML Kit OCR.
2. **Configure**: Choose Prelims MCQs, Mains 10/15-Markers, or Both, select a subject focus tag, and optionally edit the prompt.
3. **Generate**: Tap **ChatGPT →**, **Gemini →**, or **Share to Any AI App**, hit send in your AI app, and copy the JSON code snippet reply.
4. **Auto-Import & Revise**: Return to FolioPrep — the clipboard banner automatically detects the JSON payload for instant review, offline saving, and practice.

---

## Tech Stack

| Layer | Libraries & Tools |
| --- | --- |
| **Language & UI** | Kotlin, Jetpack Compose, Material 3, Edge-to-Edge, Adaptive Navigation |
| **Architecture** | MVVM (`ViewModel`, `StateFlow`, `collectAsStateWithLifecycle`), Navigation Compose |
| **Local Persistence** | Room (KSP) for decks/MCQs/flashcards/attempts, DataStore Preferences for exam years & settings |
| **Local Pre-Processing** | OkHttp + Jsoup (article extraction), Bundled ML Kit Text Recognition (on-device OCR) |
| **Media & Images** | Android Photo Picker (`PickMultipleVisualMedia`), FileProvider Camera Capture, Coil |

---

## Building from Source

### Prerequisites
- **Android Studio** (Meerkat / Narwhal or newer) with **JDK 21**
- **Android SDK 36** (`compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`)
- **Gradle 9.3+** installed locally (or open directly in Android Studio)

### Local Debug Build
```bash
git clone https://github.com/<your-username>/folioprep.git
cd folioprep
gradle :app:assembleDebug
```

The debug APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit Tests
```bash
gradle :app:testDebugUnitTest --tests "com.example.JsonPayloadParserTest" --tests "com.example.ExampleUnitTest"
```

### Building a Signed Release APK / AAB
Configure your upload keystore via environment variables (never commit keystores to Git):

```bash
export KEYSTORE_PATH="/path/to/my-upload-key.jks"
export STORE_PASSWORD="your-store-password"
export KEY_PASSWORD="your-key-password"

gradle :app:assembleRelease
# or for Google Play App Bundle:
gradle :app:bundleRelease
```

---

## Permissions & Privacy

FolioPrep is built to be **local-first and private**:
- **No backend server, analytics, or user accounts required.** All saved decks, scores, exam year settings, and custom skills stay strictly on your device.
- **Zero-permission media selection** using the native Android Photo Picker.
- **Camera permission** is requested only at runtime if you choose to photograph notes for on-device OCR.
- **Internet permission** is used solely when you explicitly tap *Extract Article* on a URL.

---

## Contributing

Contributions, bug reports, and UPSC workflow suggestions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## License

Released under the [MIT License](LICENSE).

---

Developed by **Ayush Kumar · FolioPrep Studio**.
