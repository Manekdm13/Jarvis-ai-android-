# JARVIS Android — phone-first assistant prototype

Native Kotlin + Jetpack Compose Android application. The current main-branch app version is **0.2.0**. This is an evolving prototype, not yet a complete hands-free or full-device-control assistant.

## Verified capabilities on main

- Chat-style Android interface.
- Android speech-recognition intent and Text-to-Speech replies (press-to-talk; no wake-word service yet).
- HTTPS chat client for the Cloudflare Worker AI gateway.
- Task-aware online provider routing in the Worker: Gemini, Groq, OpenRouter, and Cloudflare Workers AI. Only configured providers are attempted.
- App-to-Worker bearer authentication. The app token is encrypted locally using Android Keystore-backed AES-GCM; provider API keys remain Worker-side secrets.
- Device-local conversation history encrypted with Android Keystore.
- Starter Android phone actions prepared as intents and gated by a separate, one-time in-app approval. This is not general screen automation.
- In-app **Check for updates** control. It checks the latest GitHub release, downloads the APK, and opens Android's package installer. Android still requires the owner to approve installation; JARVIS does not silently install updates.
- Signed release workflow and published [JARVIS v0.2.0 APK](https://github.com/Manekdm13/Jarvis-ai-android-/releases/tag/v0.2.0).

## Current limitations / not implemented on main

- **Offline on-device AI:** not implemented. Cloudflare Workers AI is a cloud service and requires internet; it is not an offline fallback.
- **Full phone control:** no general-purpose AccessibilityService automation for reading screen elements, tapping, scrolling, or typing.
- Wake-word / always-listening service.
- Notification listener and notification summarisation.
- Complete local reminder/background task manager.
- Scholarship application workspace and browser automation.
- GeoGuessr screenshot analysis and geography training.
- xAI/Grok provider integration. Adding an `XAI_API_KEY` Worker secret alone will not enable Grok until the Worker implements that provider.
- Physical-device validation on the owner's Redmi Note 13.

The premium dashboard UI, scholarly search module, and editable personal-profile memory are being developed in separate open pull requests; they are not yet part of main.

## Online AI gateway setup

See [worker/README.md](worker/README.md) for Cloudflare deployment, provider secrets, bearer-token setup, routing order, and privacy notes. The presence of Worker source code and passing CI tests does not by itself prove that a live Worker has been deployed or that live provider secrets are configured.

## Permission and approval policy

- JARVIS does not grant Android permissions to itself.
- Accessibility, notification access, microphone, and other special access must be explicitly enabled by the owner in Android settings after an in-app explanation.
- Every supported consequential phone action requires a separate **Approve once** tap; **Deny** cancels it.
- Sensitive actions such as sending, posting, deleting, purchases, changing security settings, or transmitting personal data must receive a fresh confirmation immediately before execution.
- No permission bypass, silent privilege escalation, hidden recording, or covert background control.

## Privacy and security

Conversation history and the app's gateway bearer token are stored locally using Android Keystore-backed encryption. Provider API keys must never be placed in Android source code, APK assets, logs, or GitHub files; store them as Cloudflare Worker secrets. Online chat sends the message and recent conversation context to the configured provider, so do not send passwords, OTPs, or other secrets.

## Build and release

- Debug build and automated emulator/Worker checks: [GitHub Actions](https://github.com/Manekdm13/Jarvis-ai-android-/actions).
- Latest signed release: [v0.2.0](https://github.com/Manekdm13/Jarvis-ai-android-/releases/tag/v0.2.0).
- Build locally with JDK 17, Android SDK, and `gradle assembleDebug`.

A successful CI build or emulator test is not a substitute for installing and validating the APK on the owner's physical Android phone.
