# JARVIS Android — phone-first assistant prototype

Native Kotlin + Jetpack Compose Android application. The intended product runs on the owner's physical Android phone; GitHub Actions/emulators are development checks only.

## Current prototype capabilities
- Chat-style interface
- Android speech-recognition intent and Text-to-Speech replies (press-to-talk, not wake word)
- Starter phone actions prepared as Android intents and gated by a separate, one-time in-app approval
- Denying an action cancels it; approvals are not remembered or reused
- Device-local conversation history encrypted with Android Keystore
- GitHub Actions workflow for a debug APK and automated tests

## Not implemented yet
- Online AI provider/backend or offline on-device language model
- Wake-word detection / always-on voice service
- General-purpose accessibility automation
- Notification listener or notification processing
- User-selected file/document workflows
- In-app scheduled reminder/background-task manager
- End-to-end WhatsApp/Instagram message sending
- Scholarship application workspace and browser automation
- Code-change assistant, signed release APK, and in-app update mechanism

## Phone-first development plan
See [JARVIS_PHONE_FIRST_ROADMAP.md](JARVIS_PHONE_FIRST_ROADMAP.md) for the eight-capability plan, delivery sequence, physical-device definition of done, and owner-controlled permission requirements.

## Permission and approval policy
- The app does not request or enable Accessibility Service access automatically.
- Every supported consequential phone action requires a separate **Approve once** tap; **Deny** cancels it.
- Approval applies only to the exact prepared action; it is not a standing grant.
- Runtime permissions and special app access remain controlled by Android system settings and permission prompts. JARVIS must explain the purpose and wait for the owner to grant them.
- No permission bypass, silent privilege escalation, hidden recording, hidden background actions, or self-granting permissions.
- Future accessibility and notification features must be opt-in, visibly disclosed, narrowly scoped, and revocable.

## Privacy and security
Conversation history is stored in app-private preferences and encrypted at rest with Android Keystore. It is not synced to a server. Clearing app data removes local history. Never place API keys, GitHub tokens, passwords, or signing keys in the Android app source or Git repository; use a secure backend and managed secrets.

## Build
Run `gradle assembleDebug` with JDK 17 and Android SDK installed, or use the included GitHub Actions workflow.

This is a development prototype, not a finished JARVIS assistant. A successful emulator test or APK build does not prove compatibility on the owner's physical phone; physical-device verification is required before claiming a feature is complete.
