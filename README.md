# JARVIS Android — phone-first assistant prototype

Native Kotlin + Jetpack Compose Android application. The current main-branch app version is **0.2.0**. JARVIS is an evolving prototype, not yet a complete offline, hands-free, or full-device-control assistant.

## Verified capabilities on main

- Premium dark dashboard with an animated JARVIS core.
- Chat-style Android interface.
- Android speech-recognition intent and Text-to-Speech replies (press-to-talk; no wake-word service yet).
- HTTPS chat client for the Cloudflare Worker AI gateway.
- Task-aware online provider routing in the Worker: Gemini, Groq, OpenRouter, and Cloudflare Workers AI. Only configured providers are attempted.
- App-to-Worker bearer authentication. The app token is stored locally with Android Keystore-backed AES-GCM encryption; provider API keys remain Worker-side secrets.
- Gateway connection test in the app. It checks HTTPS reachability and compares a short one-way token fingerprint without displaying the token itself.
- Device-local conversation history encrypted with Android Keystore, with a searchable **Remember** screen.
- Opt-in encrypted personal profile. The suggested profile is display-only until the owner explicitly saves it; only the saved profile is included in online chat context. It can be edited or cleared.
- User-directed web research through the authenticated Worker proxy. Cloudflare Browser Run is the default read-only public-page research path; the optional TinyFish integration is a legacy fallback and requires its own Worker secret.
- User-enabled Android Accessibility Service for explicit screen-text inspection, scrolling, Back/Home navigation, and text-matched taps gated by a one-time in-app approval. This is a limited UI-control bridge, not unrestricted device administration.
- User-enabled Notification Listener Service that keeps up to 30 recent notification summaries in process memory only; notification contents are surfaced only when the user asks JARVIS to check notifications.
- Scoped gallery index for images/videos (names, sizes and dates only), plus user-selected folder access through Android's Storage Access Framework. JARVIS can list top-level shared-folder files and read plain-text/JSON files; other document types are opened with their associated Android app.
- In-app **Check for updates** control. It checks the latest GitHub release, downloads the APK, and opens Android's package installer. Android still requires the owner to approve installation; JARVIS does not silently install updates.
- Signed release workflow and published [JARVIS v0.2.0 APK](https://github.com/Manekdm13/Jarvis-ai-android-/releases/tag/v0.2.0).

## Current limitations / not implemented on main

- **Offline on-device AI:** not implemented. Cloudflare Workers AI is an online service and is not an offline fallback. The Redmi Note 13 4G model/runtime plan is still in draft PR [#19](https://github.com/Manekdm13/Jarvis-ai-android-/pull/19).
- **Full phone control:** limited accessibility actions are implemented (screen text, scroll, Back/Home, approved text-matched taps). General autonomous multi-step app workflows, robust typing, gesture automation, and per-app integrations are not yet implemented.
- Wake-word / always-listening service.
- Notification summarisation is currently a local digest; AI-powered contextual notification reasoning and reply workflows are not yet implemented.
- Complete local reminder/background task manager.
- Scholarship application workspace.
- GeoGuessr screenshot analysis and geography training.
- xAI/Grok provider integration. Adding an `XAI_API_KEY` Worker secret alone will not enable Grok; provider code, routing, health reporting, and tests are required.
- Physical-device validation on the owner's Redmi Note 13 4G.

## Online AI gateway setup

See [worker/README.md](worker/README.md) for Cloudflare deployment, provider secrets, bearer-token setup, routing order, Browser Run configuration, and privacy notes. The presence of Worker source code and passing CI tests does not by itself prove that a live Worker has been deployed or that live provider secrets are configured.

## Permission and approval policy

- JARVIS does not grant Android permissions to itself.
- Accessibility and notification access must be explicitly enabled by the owner from JARVIS Connection settings → Device Access. Android owns these special-access grants.
- Every supported consequential phone action requires a separate **Approve once** tap; **Deny** cancels it.
- Sensitive actions such as sending, posting, deleting, purchases, changing security settings, or transmitting personal data must receive a fresh confirmation immediately before execution.
- No permission bypass, silent privilege escalation, hidden recording, or covert background control.
- The current Browser Run research path is read-only; it does not sign in, submit forms, purchase items, or change website data.

## Privacy and security

Conversation history, the optional personal profile, and the app's gateway bearer token are stored locally using Android Keystore-backed encryption. Provider API keys must never be placed in Android source code, APK assets, logs, or GitHub files; store them as Cloudflare Worker secrets. Online chat sends the message and recent conversation context, including a saved profile if enabled, to the configured provider. Do not send passwords, OTPs, or other secrets.

## Build and release

- Debug build and automated emulator/Worker checks: [GitHub Actions](https://github.com/Manekdm13/Jarvis-ai-android-/actions).
- Latest signed release: [v0.2.0](https://github.com/Manekdm13/Jarvis-ai-android-/releases/tag/v0.2.0).
- Build locally with JDK 17, Android SDK, and `gradle assembleDebug`.

A successful CI build or emulator test is not a substitute for installing and validating the APK on the owner's physical Android phone.
