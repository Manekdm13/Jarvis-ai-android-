# JARVIS phone-first capability roadmap

## Product goal

Build JARVIS as a native Android assistant that runs on the owner's physical phone. GitHub Actions and emulators are development checks only; they are not the product runtime or acceptance target.

The app is currently a small Kotlin/Jetpack Compose prototype. It has a chat screen, Android speech-recognition intent, Text-to-Speech, a small allowlist of Android intents gated by a one-time in-app approval, and encrypted local conversation history. It does not yet have an AI provider, wake-word service, accessibility automation, notification listener, file workflow, reminder scheduler, or release/update flow.

## Non-negotiable safety model

1. **Least privilege:** request a permission only when the user invokes a feature that needs it. Explain the purpose before opening Android's permission/settings screen.
2. **No permission bypass:** never root the device, exploit Android, silently enable special access, or evade another app's security controls.
3. **Action proposals, not arbitrary execution:** the assistant converts an intent into a typed, allowlisted action proposal. The user sees the target, exact effect, and data involved.
4. **Fresh approval for sensitive actions:** sending a message, submitting a scholarship form, deleting or sharing a file, placing a call, making a purchase, changing security settings, or transmitting personal data requires a separate confirmation immediately before execution. Approval is one-time and bound to the exact action and payload.
5. **Read-only by default:** notification and document access is limited to content the user selected or explicitly enabled. Do not collect all notifications or scan all files by default.
6. **Visible background behavior:** microphone use must have a clear opt-in, Android-required foreground-service notification where applicable, and an obvious stop control. No hidden recording.
7. **Secrets stay out of the app:** no provider keys, GitHub tokens, passwords, or scholarship portal credentials in source code, APK assets, logs, or repository files.
8. **Auditable changes:** code-writing features create a proposed patch/branch, run tests, show a diff, and wait for the owner's approval before merging or installing. No silent self-modification.

## Capability plan

### 1. Voice conversations and wake-word activation
- Keep press-to-talk as the dependable baseline.
- Add a conversation engine behind a provider interface so online and optional on-device models can be swapped without exposing keys in the APK.
- Treat wake-word detection as a separate, opt-in milestone. On Android it may require a foreground microphone service, persistent notification, battery use, and device-specific testing. Do not claim always-listening support until it is implemented and verified on a physical device.
- Show microphone state and provide a stop/pause control.

### 2. Open and control phone apps
- Expand the current allowlisted Android-intent dispatcher.
- Prefer public Android intents, app links, and system pickers over UI automation.
- Accessibility automation is optional, separately disclosed, user-enabled in Android Settings, limited to explicitly supported tasks, and never used to bypass app security or captchas.
- Keep a visible action preview and one-time approval for consequential actions.

### 3. WhatsApp and Instagram assistance
- Start with opening the requested app or conversation through supported links and Android share intents.
- Prepare message text for user review. Do not promise generic background sending: consumer apps do not provide a universal public API for arbitrary personal-account messaging.
- Any supported send flow must show recipient and exact message, then require immediate confirmation. Respect platform rate limits and anti-abuse controls.

### 4. Selected notification access
- Use Android's user-enabled NotificationListenerService only after a clear explanation and explicit opt-in in system settings.
- Offer app-level allowlists and pause/revoke controls.
- Process only selected notifications; avoid retaining notification bodies by default. Never expose OTPs, authentication codes, or sensitive previews to an external model.

### 5. Files and documents
- Use Android Storage Access Framework pickers so the user selects individual files or folders.
- Retain only user-granted URI access needed for a chosen workflow; avoid broad storage permissions.
- Show file name, destination, and operation before writing, moving, sharing, or deleting.

### 6. Scholarship applications
- Build a scholarship workspace for opportunity tracking, eligibility checks, deadlines, document checklists, and drafts.
- Use browser/custom-tab navigation and supported upload pickers; do not store portal passwords.
- Autofill only fields the user has reviewed. Never submit an application, certify declarations, pay a fee, or send personal data without a final explicit confirmation.
- Keep application facts sourced from user-approved profile data and official scholarship requirements.

### 7. Reminders and background tasks
- Use WorkManager for deferrable background work and Android notifications for results.
- Use exact alarms only where genuinely needed and only after explaining the special access requirement.
- Show scheduled tasks, next run time, cancellation, and execution history. Respect Android background limits and battery controls.

### 8. Code improvement
- Allow JARVIS to propose code changes through a controlled development workflow: create a branch, produce a diff, run static checks/tests/build, and summarize risks.
- Require owner approval before merge, release signing, installation, or deployment.
- Never let a model execute arbitrary shell commands on the phone or access signing keys and repository secrets.

## Delivery sequence

1. **Phone foundation:** verify app launch and core chat on a physical Android device; improve permission rationale/settings entry points; preserve encrypted local memory and per-action approval.
2. **Useful low-risk actions:** app launching, selected-file import/export, local reminders, and explicit share-sheet handoff.
3. **Conversation engine:** secure provider interface, backend-held credentials, error handling, privacy controls, and optional offline model evaluation.
4. **Opt-in integrations:** notification listener and narrowly scoped accessibility actions, each with separate settings and tests.
5. **Messaging and scholarship workflows:** recipient/payload preview, draft-first workflows, and final confirmation gates.
6. **Wake word and reliability:** opt-in foreground microphone service, notification controls, battery testing, and physical-device validation.
7. **Code assistant and releases:** reviewed patch pipeline, reproducible builds, signing strategy, and safe update mechanism.

## Definition of done

A feature is not complete because it compiles or passes an emulator test. It must:
- build successfully in CI;
- have relevant automated tests;
- be installed and exercised on the target physical Android phone;
- request only the permissions it needs;
- demonstrate denial/cancellation behavior;
- document device/Android-version limitations; and
- never claim a capability that has not been verified.

## Current blockers / owner actions

- Physical-device testing requires the owner to install the debug APK and report the Android version/device model and observed behavior.
- Special Android access (Accessibility, notification listener, microphone foreground service) must be enabled by the owner in system settings after reviewing the in-app explanation.
- An online model requires a secure backend/provider choice and server-side secret management; never paste API keys into this repository or chat.
