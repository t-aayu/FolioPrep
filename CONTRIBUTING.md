# Contributing to FolioPrep

Thank you for your interest in contributing to **FolioPrep**! Contributions that improve the UPSC CSE study workflow, parser resilience, accessibility, or offline performance are warmly welcomed.

## Getting Started

1. **Fork** the repository and clone your fork locally.
2. Open the project in **Android Studio** (JDK 11+, Android SDK 36).
3. Create a feature or fix branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```

## Coding Guidelines

- Write all application code in **Kotlin** using **Jetpack Compose** and **Material 3**.
- Keep FolioPrep **local-first**: do not introduce mandatory cloud accounts, trackers, or hardcoded API keys.
- Use `MaterialTheme.colorScheme` and `Theme.kt` tokens rather than hardcoded hex colors inside composables.
- Include meaningful `contentDescription` values on icons/images and preserve `48.dp` minimum touch targets.
- Add `Modifier.testTag(...)` on new primary interactive components.
- Keep prompt calibration exclusively tailored to **UPSC CSE** (Prelims & Mains).

## Running Tests Before Submitting

Run the JVM unit test suite and ensure a clean debug build:

```bash
gradle :app:testDebugUnitTest --tests "com.example.JsonPayloadParserTest" --tests "com.example.ExampleUnitTest"
gradle :app:assembleDebug
```

If you modify `JsonPayloadParser` or `UpscPromptBuilder`, please include corresponding unit tests in `app/src/test/java/com/example/`.

## Pull Request Process

1. Ensure your branch is up to date with `main`.
2. Fill out the Pull Request template clearly, including screenshots or screen recordings for UI changes (both Light and Dark modes if applicable).
3. Reference any related issue numbers (e.g., `Fixes #12`).
