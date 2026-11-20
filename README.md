# JARVIS Android — starter prototype

Native Kotlin + Jetpack Compose Android application scaffold.

## Current prototype capabilities
- Chat-style interface
- Android speech recognition intent and Text-to-Speech replies
- Starter phone actions are prepared as Android intents and require a separate, one-time in-app approval before launch
- Denying an action cancels it; approvals are not remembered or reused
- Device-local conversation history encrypted with Android Keystore
- GitHub Actions workflow for a debug APK

## Not connected yet
- Online AI provider/backend
- Offline on-device language model
- Accessibility-service automation and advanced Android controls (service is registered, but screen interaction is not implemented)
- Signed release APK and in-app update mechanism

## Permission and approval policy
- The app does not request or enable Accessibility Service access automatically.
- Every supported phone action requires a separate **Approve once** tap; **Deny** cancels it.
- Approval only launches the specific prepared Android intent. It is not a standing grant.
- Android runtime permissions and special app access remain controlled by Android system settings and permission prompts. JARVIS must explain the purpose and wait for the user to grant them.
- No permission bypass, silent privilege escalation, hidden background actions, or self-granting permissions.
- Future accessibility features must be opt-in, visibly disclosed, and remain subject to per-action approval for sensitive operations.

## Privacy and security
Conversation history is stored in app-private preferences and encrypted at rest with Android Keystore. It is not synced to a server. Clearing app data removes local history. Never place API keys in the Android app source or Git repository; use a secure backend.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed, or use the included GitHub Actions workflow.

This is a development prototype, not a finished JARVIS assistant.

## Accessibility setup milestone

The app now declares an optional JARVIS Accessibility Service and includes an in-app shortcut to Android Accessibility settings plus an enabled/disabled status check. The service is intentionally inert in this milestone: it does not retrieve screen content, tap controls, type, or automate other apps. The user must enable it manually in Android settings. This is a permission-registration and setup foundation, not full phone control.

Before adding interaction features, implement explicit user-command routing, per-action confirmation for sensitive actions, visible activity feedback, and tests on supported physical devices. Never bypass Android permission prompts or use the service for covert monitoring.
