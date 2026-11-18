# JARVIS Android — starter prototype

Native Kotlin + Jetpack Compose Android application scaffold.

## Current prototype capabilities
- Chat-style interface
- Android speech recognition intent (uses an installed recognition service)
- Android Text-to-Speech replies
- Starter phone actions through Android intents (Settings, Wi-Fi, Bluetooth, camera, dialer, alarms, Maps, web search, and selected installed apps)
- Device-local conversation history encrypted with an AES key held by Android Keystore (last 100 chat lines)
- App data backup disabled so the local encrypted history is not copied without its device key
- GitHub Actions workflow to build and upload a debug APK

## Not connected yet
- Online AI provider/backend (requires a secure backend and user-supplied provider credentials)
- Offline on-device language model (device RAM/storage/model compatibility must be checked)
- Advanced Android controls that require explicit runtime permissions or special access
- Signed release APK and in-app update mechanism

## Privacy and security
Conversation history is stored in the app's private preferences and encrypted at rest with Android Keystore. It is not synced to a server. If the app's Keystore key becomes unavailable, the unreadable saved history is discarded rather than crashing the app. Clearing app data removes local history. Never place API keys in the Android app source or Git repository; use a secure backend.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed, or use the included GitHub Actions workflow.

This is a development prototype, not a finished JARVIS assistant.
