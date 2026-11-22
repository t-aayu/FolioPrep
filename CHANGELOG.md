# Changelog

All notable changes to **FolioPrep** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-22

### Added
- **Local Pre-Processing Studio**
  - Direct text input with automatic ~2,500-word chunking for long notes and articles.
  - Local article link extractor using OkHttp and Jsoup to strip navigation and page clutter.
  - On-device OCR via bundled Google ML Kit Text Recognition using Android Photo Picker and optional Camera capture.
  - Automatic clipboard paste into empty Article URL and Source Text inputs.
- **UPSC CSE Exam Pattern Persistence**
  - Pinned **Latest Prelims Year** and **Latest Mains Year** selectors at the top of the Library homepage, stored permanently via DataStore Preferences.
  - Automatic prompt calibration to the selected Prelims/Mains year and all previous UPSC CSE years.
- **UPSC CSE Question Setter Prompt Builder**
  - Default **UPSC CSE Question Setter** (seasoned university professor) persona with syllabus and PYQ correlation rules.
  - Mode-specific UPSC CSE subject tags for Prelims MCQ, Mains 10/15-Markers, and Both.
  - Editable Prompt Editor with pretty-printed ` ```json ` schema output and multi-line statement formatting (`\n`).
  - Highlighted **ChatGPT →** and **Gemini →** quick-send pills alongside Android Sharesheet and Copy Prompt actions.
- **Auto-Clipboard Import & Resilient JSON Parsing**
  - Window-focus clipboard detection banner (`onWindowFocusChanged`) on return from external AI apps.
  - Quote-aware JSON parser supporting fenced code blocks, pretty-printed indentation, multi-line question stems, markdown-escaped punctuation, and trailing commas.
  - Interactive **Verify & Save Deck** preview screen with inline editing for titles, tags, MCQs, and Mains flashcards.
- **Offline Room Library & Study Modes**
  - Subject tag filtering and instant search across saved decks.
  - **Prelims Practice Mode** with instant answer reveal, Question Setter explanations, and attempt history.
  - **Timed Prelims Exam Mode** with live countdown timer, question palette, and configurable negative marking (including UPSC `-1/3`).
  - **Mains 10/15-Mark Flashcards** with 3D flip animation showing Introduction, Core Arguments, and Conclusion / Way Forward with mastery tracking.
- **Settings Screen**
  - Persistent **Light / Dark / System** appearance switch (Warm Editorial Paper & Warm Ink themes).
  - Toggle between **Default (`UPSC CSE Paper Setter`)** and **Custom Skill**, with an expandable Custom Skill editor and save action.
  - App version and developer information section.
