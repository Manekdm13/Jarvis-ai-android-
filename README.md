# JARVIS Android — starter prototype

Native Kotlin + Jetpack Compose Android application scaffold.

## Current prototype capabilities
- Chat-style interface
- Android speech recognition intent and Text-to-Speech replies
- Starter phone actions are prepared as Android intents and require a separate, one-time in-app approval before launch
- Denying an action cancels it; approvals are not remembered or reused
- Device-local conversation history encrypted with Android Keystore
- GitHub Actions workflow for a debug APK

## Online AI brain (new integration)
- Android chat can connect to a configurable HTTPS Cloudflare Worker gateway.
- Gateway tries Gemini 3.8 Flash first and OpenRouter's free-model router as fallback.
- Provider keys stay in Worker secrets, never in the APK or repository.
- See `worker/README.md` for deployment, free-tier privacy notes, and setup.

## Not connected yet
- Live provider credentials and deployed Worker (requires account setup)
- Offline on-device language model
- Accessibility-service automation and advanced Android controls
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
