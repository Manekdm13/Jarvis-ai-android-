# JARVIS 3.0 — 21-Section Implementation Plan

**Purpose:** turn the 21-section interface into a dependable Android personal AI system. A screen, prompt, or model response is not evidence that a capability is implemented.

## Engineering rules

- Keep provider credentials on the Cloudflare Worker. Android stores only the user-specific gateway token using Android Keystore-backed encryption.
- Use Kotlin coroutines, repository interfaces, ViewModels/state holders, and explicit domain models. Keep UI composables free of network and persistence logic.
- Persist conversations, profile memory, and task state with encrypted storage / Room as appropriate. Migrate existing encrypted preferences before deleting or replacing them.
- Request runtime permissions just in time, with a clear rationale and a graceful denied-permission state.
- Require a visible, specific approval for sensitive or irreversible actions. Never treat a model response as proof that an action happened.
- Use WorkManager for deferrable work; use foreground services only for user-visible ongoing work that meets Android policy. Respect Doze, battery restrictions, and background-start limits.
- Add structured logs with secrets and personal content redacted. Add crash reporting only with privacy disclosure and configuration.
- Every capability must have a testable acceptance criterion and an observable success/failure result.

## Android platform corrections

- **Files:** use Storage Access Framework (OpenDocument, OpenDocumentTree) and persist URI grants. Do not depend on broad legacy storage permissions.
- **Photos/videos:** prefer Android Photo Picker. For broad media indexing, use version-appropriate READ_MEDIA_* / selected-visual-media permissions and explain the scope.
- **Health/Fitness:** integrate Health Connect with its specific read/write permissions and availability checks; BODY_SENSORS is not a substitute.
- **Calls:** prefer ACTION_DIAL (no CALL_PHONE permission). Direct calling is a separate high-risk capability requiring explicit permission and confirmation.
- **Calendar:** use Calendar Provider permissions only if direct provider access is required; otherwise use calendar intents or a supported connector.
- **App discovery:** use package visibility queries for known targets. Avoid QUERY_ALL_PACKAGES unless policy eligibility and a genuine core use case are established.
- **Notifications/Accessibility:** protected service binding permissions are declarations, not runtime grants. The user must enable each service in Android Settings. Explain exactly what content can be read or acted on and provide an obvious off switch.
- **Wi-Fi/Bluetooth:** modern Android restricts direct toggling. Prefer system settings panels/intents and user-mediated flows; request only API-level permissions actually required.
- **Device administration:** DevicePolicyManager features requiring device-owner/profile-owner privileges are not ordinary app permissions and must not be promised to a regular installation.
- **Developer Studio:** do not execute arbitrary downloaded code in-process. Use a sandboxed remote build/test service or a constrained local environment with explicit user approval.

## 21-section acceptance matrix

| # | Section | First real capability to deliver | Acceptance evidence |
|---|---|---|---|
| 1 | Command Centre | truthful service/provider/task status | status derived from live checks, not a static ONLINE label |
| 2 | AI Brain | authenticated gateway chat, provider fallback, timeouts | success/failure integration tests; provider identity and latency |
| 3 | Voice Assistant | permission-aware speech input and TTS | on-device test for grant, denial, interruption, and stop |
| 4 | Personal Memory | encrypted conversation/profile memory with user controls | persistence, retrieval, clear, corruption, and opt-in tests |
| 5 | Autonomous Agent | durable plan/task state machine and tool executor | multi-step plan, restart recovery, approval gate, retry tests |
| 6 | Phone Control | allowlisted Android intents with per-action approval | tests for approve, deny, cancel, and unavailable intents |
| 7 | Communication | safe compose/deep-link flows before send integrations | no message sent without review; connector contract tests |
| 8 | Research Lab | web research with source URLs and citations | source-grounded result tests and network failure handling |
| 9 | Knowledge Centre | searchable local knowledge store | indexing, retrieval relevance, deletion, and migration tests |
| 10 | Finance & Markets | clearly attributed market-data provider | stale-data labels, timestamp, rate-limit and provider-failure tests |
| 11 | Health & Medicine | informational assistant plus user-authorized Health Connect reads | explicit scope, consent, source timestamps; no diagnosis claims |
| 12 | Fitness & Nutrition | user-authorized activity/nutrition records | permission denial, units, date range, and provenance tests |
| 13 | Food Science | specialist workflows and source-backed calculations | calculation fixtures, citations, and unit validation |
| 14 | Education | study plans, notes, quizzes, spaced repetition | plan persistence and answer/rubric tests |
| 15 | Scholarship Manager | verified opportunity records, eligibility filters, deadlines | official source URL, checked date, eligibility reason, reminder tests |
| 16 | Files & Gallery | user-selected files and supported text extraction | URI permission persistence, unsupported-file and revoke tests |
| 17 | Productivity | task/reminder/calendar intent flows | durable task state and no duplicate scheduling |
| 18 | Security Centre | permission/service audit and local privacy controls | accurate permission state, redaction, clear-data tests |
| 19 | Developer Studio | repository/task workspace and isolated build integration | no arbitrary in-app execution; build logs and artifact provenance |
| 20 | Internet & Tools | allowlisted tool registry and web-agent gateway | auth, timeout, domain policy, approval, and result verification |
| 21 | Self-Improvement | user-approved feedback and evaluation loop | versioned preference changes, regression suite, rollback; no silent self-modifying code |

## Delivery sequence

### Phase 0 — reliability and access
1. Fix narrow-screen navigation and make Settings directly reachable.
2. Replace static connection indicators with real gateway state.
3. Verify the gateway token fingerprint and a real authenticated chat request.
4. Add structured error states, redacted diagnostics, and regression tests.

### Phase 1 — secure foundations
1. Establish repository / ViewModel boundaries and typed results.
2. Introduce Room migrations for durable tasks, memories, and knowledge where justified.
3. Add a tool registry with capability declarations, permission checks, approval policies, timeouts, and audit events.
4. Add offline-safe local capabilities and explicit online/offline state.

### Phase 2 — useful execution
1. Implement task state transitions: planned → ready → in progress → waiting for approval → completed / failed / cancelled.
2. Add restart recovery, idempotency keys, bounded retries, and user-visible progress.
3. Connect Android intents, selected files, notifications, and accessibility only after opt-in.
4. Add research and scholarship source tracking before adding more integrations.

### Phase 3 — specialist integrations
Deliver each specialist domain through provider interfaces and feature-specific tests. Do not ship fake data or imply external integrations exist when they do not.

### Phase 4 — hardening
Run unit tests, Android instrumentation tests, emulator smoke tests, gateway contract tests, dependency/security checks, and a physical-device checklist. Publish an APK only when its commit SHA, build, test results, and artifact are linked.

## Release gates

- Clean Gradle build succeeds.
- Unit and instrumentation tests pass.
- Emulator launch and navigation tests pass at narrow and standard screen sizes.
- Gateway health and authenticated chat are verified independently.
- No secrets appear in source, logs, crash reports, or APK resources.
- All approval/denial paths are tested.
- APK is built from the exact reported commit and its checksum is recorded.
- Physical-device-only behavior is clearly marked NOT VERIFIED until tested on the Redmi Note 13 4G.

## Status vocabulary

- **IMPLEMENTED AND TESTED** — working behavior with repeatable passing evidence.
- **IMPLEMENTED BUT NOT TESTED** — code exists; runtime behavior remains unverified.
- **PARTIALLY WORKING** — only a subset or constrained path works.
- **UI/PROMPT ONLY** — screen or model instructions exist without the claimed integration.
- **NOT IMPLEMENTED** — no working implementation.
- **BLOCKED** — external credentials, platform restrictions, or missing user authorization prevent validation.

Do not calculate overall completion by counting screens. Score tested acceptance criteria, and publish evidence and exclusions alongside any percentage.
