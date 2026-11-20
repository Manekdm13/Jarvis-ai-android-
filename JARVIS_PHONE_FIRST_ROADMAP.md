# JARVIS phone-first capability roadmap

## Product goal

Build JARVIS as a modular personal AI assistant that runs on the owner's physical Android phone. GitHub Actions and emulators are development checks only; they are not the product runtime or acceptance target. The capability list is an extensible roadmap, not a fixed limit: new modules can be proposed and added without restricting JARVIS to the items currently listed.

The app is currently a small Kotlin/Jetpack Compose prototype. It has a chat screen, Android speech-recognition intent, Text-to-Speech, a small set of Android intents gated by one-time in-app approval, and encrypted local conversation history. It does not yet have an AI provider, wake-word service, accessibility automation, notification listener, file workflow, reminder scheduler, or release/update flow.

## Non-negotiable safety model

1. **Least privilege:** request a permission only when the user invokes a feature that needs it. Explain the purpose before opening Android's permission/settings screen.
2. **No permission bypass:** never root the device, exploit Android, silently enable special access, or evade another app's security controls.
3. **Action proposals, not arbitrary execution:** the assistant converts an intent into a typed, allowlisted action proposal. The user sees the target, exact effect, and data involved.
4. **Fresh approval for sensitive actions:** sending a message, submitting a scholarship form, deleting or sharing a file, placing a call, making a purchase, changing security settings, or transmitting personal data requires a separate confirmation immediately before execution. Approval is one-time and bound to the exact action and payload.
5. **Read-only by default:** notification and document access is limited to content the user selected or explicitly enabled. Do not collect all notifications or scan all files by default.
6. **Visible background behavior:** microphone use must have a clear opt-in, Android-required foreground-service notification where applicable, and an obvious stop control. No hidden recording.
7. **Secrets stay out of the app:** no provider keys, GitHub tokens, passwords, or scholarship portal credentials in source code, APK assets, logs, or repository files.
8. **Auditable changes:** code-writing features create a proposed patch/branch, run tests, show a diff, and wait for the owner's approval before merging or installing. No silent self-modification.
9. **Modular and extensible:** features are separate modules with documented permissions, tests, and approval levels. The owner can request additional capabilities at any time; this roadmap is not a closed list.

## Capability plan

### 1. Voice conversations and wake-word activation
- Keep press-to-talk as the dependable baseline.
- Add a conversation engine behind a provider interface so online and optional on-device models can be swapped without exposing keys in the APK.
- Treat wake-word detection as a separate, opt-in milestone. On Android it may require a foreground microphone service, persistent notification, battery use, and device-specific testing. Do not claim always-listening support until it is implemented and verified on a physical device.
- Show microphone state and provide a stop/pause control.

### 2. Full phone navigation and control
- Make phone operation a first-class goal: JARVIS should understand multi-step requests, open the relevant app or Android screen, navigate supported interfaces, tap, scroll, type, select, and verify the visible result—similar to a person using the phone.
- Prefer official Android APIs, public app intents/deep links, accessibility node actions, and system pickers. Use narrowly scoped AccessibilityService automation only after the owner explicitly enables it in Android Settings.
- Provide a visible action plan for multi-step tasks, progress, a stop/pause control, and a clear result or failure explanation. Re-check screen state between steps; never assume a tap succeeded.
- Allowlist supported actions and app workflows. Do not bypass lock screens, captchas, payment confirmations, app security, or another person's access controls. Do not perform hidden surveillance or covert recording.
- For sensitive or irreversible steps—sending, posting, deleting, purchasing, submitting, changing security/privacy settings, or sharing personal data—show the exact target and payload and require fresh approval immediately before that step.
- Explain that Android and individual apps may block or limit automation; unsupported actions must fail safely and offer a manual handoff rather than pretending to work.

### 3. Privacy, private mode, and device security
- Create a privacy dashboard that explains each JARVIS permission and integration, why it is needed, what data it can access, and how to pause or revoke it.
- Offer user-approved privacy routines: hide sensitive notification previews where Android exposes the setting, review app permissions, open privacy/security settings, clear supported browser history/site data through the browser's own controls, and enable available screen-lock or app-lock options.
- Support a JARVIS private session that can avoid saving conversation history, minimise local retention, and clearly indicate when temporary/private mode is active. Respect Android's secure storage and encryption facilities for retained data.
- Provide guidance and supported controls for browser incognito/private tabs, app tracking/privacy settings, clipboard hygiene, backup choices, and permission reviews. Do not claim that incognito makes a user anonymous or hides activity from websites, network providers, employers, or device administrators.
- Never silently disable security protections, install certificates, configure a VPN/proxy, change DNS, hide monitoring, or weaken device security. Any system-wide privacy/security change must be explained and separately approved.
- Keep an auditable local activity history with a user-controlled retention/clear option; do not log passwords, OTPs, authentication tokens, or private message bodies unnecessarily.

### 4. WhatsApp and Instagram assistance
- Start with opening the requested app or conversation through supported links and Android share intents.
- Prepare message text for user review. Do not promise generic background sending: consumer apps do not provide a universal public API for arbitrary personal-account messaging.
- Any supported send flow must show recipient and exact message, then require immediate confirmation. Respect platform rate limits and anti-abuse controls.

### 5. Selected notification access
- Use Android's user-enabled NotificationListenerService only after a clear explanation and explicit opt-in in system settings.
- Offer app-level allowlists and pause/revoke controls.
- Process only selected notifications; avoid retaining notification bodies by default. Never expose OTPs, authentication codes, or sensitive previews to an external model.

### 6. Files and documents
- Use Android Storage Access Framework pickers so the user selects individual files or folders.
- Retain only user-granted URI access needed for a chosen workflow; avoid broad storage permissions.
- Show file name, destination, and operation before writing, moving, sharing, or deleting.

### 7. Scholarship applications
- Build a scholarship workspace for opportunity tracking, eligibility checks, deadlines, document checklists, and drafts.
- Use browser/custom-tab navigation and supported upload pickers; do not store portal passwords.
- Autofill only fields the user has reviewed. Never submit an application, certify declarations, pay a fee, or send personal data without a final explicit confirmation.
- Keep application facts sourced from user-approved profile data and official scholarship requirements.

### 8. Reminders and background tasks
- Use WorkManager for deferrable background work and Android notifications for results.
- Use exact alarms only where genuinely needed and only after explaining the special access requirement.
- Show scheduled tasks, next run time, cancellation, and execution history. Respect Android background limits and battery controls.

### 9. Code improvement
- Allow JARVIS to propose code changes through a controlled development workflow: create a branch, produce a diff, run static checks/tests/build, and summarize risks.
- Require owner approval before merge, release signing, installation, or deployment.
- Never let a model execute arbitrary shell commands on the phone or access signing keys and repository secrets.

### 10. GeoGuessr mastery and geography intelligence
- Provide a geography/GeoGuessr companion mode for screenshots or images the owner deliberately shares.
- Analyse visible clues such as language, road signs and markings, driving side, road infrastructure, utility poles, architecture, vegetation, terrain, climate cues, and landscape.
- Rank likely countries/regions/locations, explain the evidence and counter-evidence, and communicate uncertainty rather than pretending to know an exact spot.
- Teach regional clue patterns, generate quizzes and practice rounds, compare similar countries/regions, and maintain an optional progress log of mistakes and strengths.
- Support map/geography learning, country and flag knowledge, capitals, regions, and spatial reasoning.
- Keep this as an assistive coach and analysis tool. Do not secretly control the game, automate competitive gameplay, exploit the game, or bypass its rules. Physical image/screenshot analysis must be tested on actual user-provided examples.

## Additional expandable modules (not a closed list)

Potential future modules can include:
- Personal knowledge base: searchable notes, documents, saved facts, and project context under user control.
- Study and tutoring: explain topics, make flashcards, quizzes, revision schedules, and track learning progress.
- Research assistant: compare sources, summarise papers, maintain citations, and flag uncertain claims.
- Calendar and productivity: organise tasks, events, routines, and focus sessions with confirmation before external changes.
- Travel and navigation: itinerary planning, translation, local phrase help, and map handoff.
- Communication assistant: draft, summarise, and organise messages/email; require approval before sending.
- Media and creativity: help plan, generate, edit, and organise content using explicitly connected tools.
- Device diagnostics: battery/storage/network status where Android APIs permit, with clear explanations and no hidden collection.
- Personal automation: user-defined routines and triggers with a visible activity log, pause switch, and per-action safety policy.
- Smart-home and external services: only through user-connected, scoped integrations with revocable permissions.

These are candidate modules, not claims that the current prototype already supports them. Add new ideas to the backlog when requested and assess feasibility, privacy, platform rules, and required permissions before implementation.

## Delivery sequence

1. **Phone foundation:** verify app launch and core chat on a physical Android device; improve permission rationale/settings entry points; preserve encrypted local memory and per-action approval.
2. **Useful low-risk actions:** app launching, selected-file import/export, local reminders, and explicit share-sheet handoff.
3. **Full-control foundation:** build an accessibility-based, allowlisted navigation engine with step verification, stop control, visible action history, and sensitive-action approval gates.
4. **Privacy foundation:** add private sessions, permission dashboard, retention controls, and supported privacy-setting handoffs.
5. **Conversation engine:** secure provider interface, backend-held credentials, error handling, privacy controls, and optional offline model evaluation.
6. **Opt-in integrations:** notification listener and narrowly scoped accessibility actions, each with separate settings and tests.
7. **Messaging and scholarship workflows:** recipient/payload preview, draft-first workflows, and final confirmation gates.
8. **Wake word and reliability:** opt-in foreground microphone service, notification controls, battery testing, and physical-device validation.
9. **GeoGuessr/geography module:** screenshot import, visual clue analysis, confidence-ranked explanations, training mode, and user-example validation.
10. **Code assistant and releases:** reviewed patch pipeline, reproducible builds, signing strategy, and safe update mechanism.
11. **Expandable modules:** prioritise additional owner-requested modules through small, independently testable releases.

## Definition of done

A feature is not complete because it compiles or passes an emulator test. It must:
- build successfully in CI;
- have relevant automated tests;
- be installed and exercised on the target physical Android phone (or validated with real user-provided content for non-device visual modules);
- request only the permissions it needs;
- demonstrate denial/cancellation behavior;
- document device/Android-version and third-party service limitations; and
- never claim a capability that has not been verified.

## Current blockers / owner actions

- Physical-device testing requires the owner to install the debug APK and report the Android version/device model and observed behavior.
- Special Android access (Accessibility, notification listener, microphone foreground service) must be enabled by the owner in system settings after reviewing the in-app explanation.
- An online model requires a secure backend/provider choice and server-side secret management; never paste API keys into this repository or chat.
