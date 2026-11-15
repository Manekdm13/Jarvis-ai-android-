# JARVIS Android — starter prototype

Native Kotlin + Jetpack Compose Android application scaffold.

## Current prototype capabilities
- Chat-style interface
- Android speech recognition intent (uses an installed recognition service)
- Android Text-to-Speech replies
- GitHub Actions workflow to build and upload a debug APK

## Not connected yet
- Online AI provider/backend (requires a secure backend and user-supplied provider credentials)
- Offline on-device language model (device RAM/storage/model compatibility must be checked)
- Android device actions beyond standard intents and permission-gated APIs
- Signed release APK and in-app update mechanism

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed, or use the included GitHub Actions workflow after the source is committed to `main`.

This is a development prototype, not a finished JARVIS assistant. Never place API keys in the Android app source or Git repository; use a secure backend.
