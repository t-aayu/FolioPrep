# FolioPrep — UPSC CSE Study Studio

[![Android CI & Release](https://github.com/t-aayu/FolioPrep/actions/workflows/android-release.yml/badge.svg)](https://github.com/t-aayu/FolioPrep/actions/workflows/android-release.yml)
[![Latest Release](https://img.shields.io/github/v/release/t-aayu/FolioPrep?label=Release)](https://github.com/t-aayu/FolioPrep/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Min Android SDK](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-brightgreen.svg)](app/build.gradle.kts)

**FolioPrep** is a local-first Android study studio and offline quiz builder built exclusively for **UPSC Civil Services Examination (CSE)** aspirants.

Extract clean text locally from articles, textbook photos, or personal notes; craft exam-calibrated prompts as a senior UPSC CSE Question Setter with configurable hardness levels (**Easy**, **Medium**, **Hard**); send them directly to your installed AI apps (**ChatGPT**, **Gemini**, **Claude**, **Perplexity**, or any Android share target); and auto-import the structured JSON reply into an offline **Room** library with automatic local folder backup for **Just Solve**, **Prelims Practice**, **Timed Prelims Simulation**, and **Mains 10/15-Mark Flashcards**.

---

## Download

Pre-built APKs and checksums (`SHA256SUMS.txt`) are published on the [GitHub Releases](https://github.com/t-aayu/FolioPrep/releases) page.

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

- **UPSC CSE Question Setter Prompt Studio & Hardness Calibration**
  - Defaults to a senior **UPSC CSE Question Setter** (seasoned university professor) persona, with a Settings toggle to switch to your own saved **Custom Skill**.
  - **Question Hardness Selector**: Choose **Easy**, **Medium** (default), or **Hard**; the prompt also instructs the AI to tag each MCQ with `"difficulty": "Easy" | "Medium" | "Hard"` in JSON for targeted revision.
  - Correlates current-affairs source material with static UPSC CSE syllabus knowledge and Previous Year Question (PYQ) trends.
  - Mode-specific subject tags:
    - **Prelims MCQ**: Polity, Economy, Environment, History, Geography, Sci-Tech, IR, Government Schemes, Current Affairs
    - **Mains 10/15M**: Geography, Indian History, World History, Indian Society, Polity and Governance, IR, Social Justice, Economy, Internal Security, Sci-Tech, Environment Biodiversity, Disaster Management, Ethics integrity and aptitude
    - **Both**: History, Geography, Polity, IR, Economy, Sci-tech, Environment
  - Full in-app Prompt Editor with monospace formatting and pretty-printed ` ```json ` schema output.

- **Seamless External AI Handoff & Resilient Auto-Import**
  - Highlighted **ChatGPT →** and **Gemini →** quick-send pills, plus one-tap Android Sharesheet and Copy Prompt support.
  - Automatic clipboard detection when returning to FolioPrep (`onWindowFocusChanged`).
  - Resilient JSON parser that handles fenced code snippets, pretty-printed indentation, multi-line UPSC statement stems (`\n` / `\n\n`), markdown-escaped punctuation (`\[ \] \{ \} \& \- \_`), difficulty tags, and trailing commas.
  - Interactive **Verify & Save Deck** preview screen to edit titles, tags, MCQ hardness levels, questions, or Mains blueprints before saving offline.

- **Offline Revision Modes, Sharing & Selectable Explanations**
  - **Just Solve (`QUICK SOLVE`)**: Jump directly into any question (or the full deck) with instant answer reveal, explanation, and a *Try Again* reset without timer pressure or score logging.
  - **Prelims Practice Mode (`UNTIMED`)**: Instant answer reveal, detailed UPSC Question Setter explanations, and score history.
  - **Timed Prelims Exam Mode (`SIMULATION`)**: Live countdown timer, jumpable question palette, and configurable negative marking (including official UPSC `-1/3`).
  - **Revise MCQs by Hardness**: Filter MCQs by **Easy**, **Medium**, or **Hard** inside any Study Dossier, or revise all Easy/Medium/Hard MCQs across your entire library from the Home screen.
  - **Share Question & Share Explanation**: Copy text to clipboard or share crisp theme-matched (Light/Dark) 1080px PNG screenshot cards for both questions (with options A–D) and explanations (with highlighted correct answer).
  - **Selectable Explanation Text**: Long-press any UPSC Question Setter explanation (during practice or in the answer key) to select and copy specific lines.
  - **Mains 10/15-Mark Flashcards**: Spring-animated 3D flip cards revealing structured **Introduction → Core Arguments → Conclusion / Way Forward** blueprints with mastery tracking.

- **Automatic Local Folder Backup & Fresh-Install Restore**
  - Automatically exports all decks, MCQs (with hardness levels), Mains flashcards, quiz history, exam years, and custom prompt skills to `Documents/FolioPrep/folioprep_backup.json` using zero-permission Scoped Storage (or a custom Android folder via Storage Access Framework), surviving app uninstallation.
  - Prompts to restore from local backup on a fresh installation when the library is empty, with **Backup Now**, **Restore**, and **Choose Custom Android Backup Folder** controls in **Settings**.

- **Editorial Design & Settings**
  - **Warm Editorial Paper** (Light), **Warm Ink** (Dark), and **System** theme switching in Settings.
  - Bundled typography (`Newsreader`, `Plus Jakarta Sans`, `JetBrains Mono`), adaptive Navigation Bar / Navigation Rail for phones/foldables/tablets, and direct GitHub repository link in Settings.

---

## How the 4-Stage Workflow Works

```text
[1. Local Extract]  ──▶  [2. Prompt Studio]  ──▶  [3. External AI]  ──▶  [4. Auto-Import & Study]
 Text / URL / OCR        Configure counts,        Tap ChatGPT →,        Copy JSON reply & return;
 cleaned on-device       mode, hardness & tags    Gemini →, or Share    verify, save & auto-backup
```

1. **Extract**: Paste text, extract an article link with Jsoup, or scan notes with on-device ML Kit OCR.
2. **Configure**: Choose Prelims MCQs, Mains 10/15-Markers, or Both, set hardness (**Easy / Medium / Hard**), pick a subject focus tag, and optionally edit the prompt.
3. **Generate**: Tap **ChatGPT →**, **Gemini →**, or **Share to Any AI App**, hit send in your AI app, and copy the JSON code snippet reply.
4. **Auto-Import & Revise**: Return to FolioPrep — the clipboard banner automatically detects the JSON payload for instant review, offline saving, automatic local folder backup, and practice.

---

## Tech Stack

| Layer | Libraries & Tools |
| --- | --- |
| **Language & UI** | Kotlin, Jetpack Compose, Material 3, Edge-to-Edge, Adaptive Navigation |
| **Architecture** | MVVM (`ViewModel`, `StateFlow`, `collectAsStateWithLifecycle`), Navigation Compose |
| **Local Persistence** | Room (KSP, v2 migration) for decks/MCQs/flashcards/attempts, DataStore Preferences for exam years & settings |
| **Local Backup & Share** | Android Scoped Storage (`MediaStore.Files`), Storage Access Framework (`OpenDocument` / `OpenDocumentTree`), `FileProvider` PNG Canvas renderer |
| **Local Pre-Processing** | OkHttp + Jsoup (article extraction), Bundled ML Kit Text Recognition (on-device OCR) |
| **Media & Images** | Android Photo Picker (`PickMultipleVisualMedia`), FileProvider Camera Capture, Coil |

---

## Building from Source

### Prerequisites
- **Android Studio** (Meerkat / Narwhal or newer) with **JDK 21**
- **Android SDK 36** (`compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`)
- **Gradle 9.3.1+** installed locally (or open directly in Android Studio)

### Local Debug Build
```bash
git clone https://github.com/t-aayu/FolioPrep.git
cd FolioPrep
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

### Publishing a GitHub Release via GitHub Actions
1. (Optional for signed release APKs/AABs) In **GitHub → Settings → Secrets and variables → Actions**, add:
   - `UPLOAD_KEYSTORE_BASE64` — base64-encoded `.jks` keystore (`base64 -w 0 my-upload-key.jks`)
   - `STORE_PASSWORD` — keystore password
   - `KEY_PASSWORD` — upload key alias password
2. Tag and push a version:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. The [Android CI & GitHub Release](.github/workflows/android-release.yml) workflow automatically runs unit tests, builds versioned APKs/AABs, generates `SHA256SUMS.txt`, and publishes a GitHub Release with categorized release notes.

---

## Permissions & Privacy

FolioPrep is built to be **local-first and private**:
- **No backend server, analytics, or user accounts required.** All saved decks, scores, exam year settings, and custom skills stay strictly on your device.
- **Zero-permission media selection & local backup**: Uses Android Photo Picker (`PickMultipleVisualMedia`), `MediaStore.Files` (`Documents/FolioPrep/folioprep_backup.json`), and Storage Access Framework (`OpenDocument` / `OpenDocumentTree`) — no broad storage permissions (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`) are ever requested.
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

Developed by **Ayush Kumar · FolioPrep Studio** · [github.com/t-aayu/FolioPrep](https://github.com/t-aayu/FolioPrep/)
