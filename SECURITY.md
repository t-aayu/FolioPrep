# Security & Privacy Policy

## Local-First Privacy Guarantees

**FolioPrep** is designed so your study materials stay under your control:
- Study decks, MCQs, Mains flashcards, score history, exam year settings, and custom prompt skills are stored **locally on your device** (Room SQLite & DataStore Preferences).
- Automatic local backups are written strictly to `Documents/FolioPrep/folioprep_backup.json` (or your chosen folder via Android Storage Access Framework) with **zero broad storage permissions**.
- On-device OCR uses bundled **Google ML Kit Text Recognition** without uploading your images to any FolioPrep server.
- Media selection uses the zero-permission **Android Photo Picker**.
- No analytics SDKs, ad networks, or third-party telemetry are bundled.

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 1.0.x   | Yes       |

## Reporting a Vulnerability

If you discover a security or privacy issue (such as unsafe URL/HTML handling, clipboard edge cases, backup parsing issues, or FileProvider URI exposure):

1. **Do not** open a public GitHub issue containing exploit details.
2. Report it privately using GitHub **Security Advisories** at [github.com/t-aayu/FolioPrep/security/advisories](https://github.com/t-aayu/FolioPrep/security/advisories) or contact the maintainer directly.
3. Include steps to reproduce, Android OS version, and affected app version.

We aim to acknowledge reports within 72 hours and ship a fix promptly.
