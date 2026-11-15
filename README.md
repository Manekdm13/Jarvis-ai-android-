# JARVIS Android — hybrid assistant

This repository contains the first native Android prototype for the JARVIS project.

## Current prototype
- Native Kotlin + Jetpack Compose Android app.
- Speech input through Android's speech recognition service.
- Spoken responses through Android Text-to-Speech.
- Basic chat interface.
- GitHub Actions workflow to build and upload a debug APK.

## Planned hybrid architecture
1. **Online brain:** authenticated HTTPS requests to a configurable AI backend for advanced reasoning.
2. **Offline brain:** on-device model runtime, selected after checking the phone's RAM/storage and model compatibility.
3. **Router:** choose online reasoning when available and local fallback when offline.
4. **Android tools:** explicit, permission-gated actions using Android APIs and intents.
5. **Memory:** user-controlled local preferences and conversation history.
6. **Updates:** versioned releases and an in-app update flow after signing/release infrastructure is configured.

## Important status
The AI provider and local model are **not connected yet**. The prototype does not silently control the phone. Sensitive actions must request Android permissions and user confirmation.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK platform 36 installed. GitHub Actions runs this build on pushes to `main` and manual workflow dispatches.
