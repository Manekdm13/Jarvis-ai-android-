# JARVIS Master Release — Completion Gate

This branch is the integration target for the master product. Do not publish an APK from this branch until every release-blocking item below has implementation evidence and a passing test. A green compile alone is not release approval.

## Existing foundations verified in source
- [x] AI gateway client and secure token storage exist.
- [x] Conversation memory, user profile memory, and encrypted preference-learning foundation exist.
- [x] WorkManager task scheduling and a bounded AI-only task planner exist.
- [x] Accessibility service supports a small set of visible UI actions; it does not bypass app sandboxes.
- [x] On-device OCR exists for a selected image, multiple selected images (bounded batch), and rendered PDF pages (bounded page count).
- [x] Image/PDF extracted text is staged for user review before it is sent to the gateway.
- [x] App updater foundation and voice/TTS settings exist.

## Release blockers — implementation required

### 1. Hands-free voice
- [ ] Integrate a real offline keyword-spotting engine and a redistributable wake-word model for the chosen wake phrase.
- [ ] Add a microphone foreground service with persistent OS notification, visible listening state, explicit start/stop, permission checks, and battery-aware behavior.
- [ ] Implement continuous speech sessions after wake, turn detection, interruption/barge-in, TTS playback, and return-to-listening behavior.
- [ ] Verify Android 14/15/16 behavior, process death recovery, lock-screen behavior, Bluetooth/headset routing, and battery use on a physical Redmi Note 13 4G.
- [ ] Never describe a normal tap-to-speak SpeechRecognizer session as an always-on wake word.

### 2. Agent execution and background work
- [ ] Replace the AI-only outline executor with a typed tool/action registry and explicit capability grants.
- [ ] Support durable multi-step plans, persisted step state, cancellation, retry/backoff, deadlines, and crash recovery.
- [ ] Separate read-only, reversible, and sensitive actions. Require a fresh, specific approval for send/post/delete/purchase/call/security changes and external transmission of private content.
- [ ] Verify every action result from Android/tool feedback; never report success merely because an intent was launched.
- [ ] Add task history, progress notifications, failure explanations, and resume controls.
- [ ] Keep long-running work within Android WorkManager/foreground-service policy; document tasks that cannot run while the app is force-stopped.

### 3. Phone, files, and app integration
- [ ] Add scoped MediaStore/Storage Access Framework indexing with user-selected grants, revocation handling, and an explicit scan scope.
- [ ] Add document-type support and bounded extraction for PDF, text, DOCX and supported image formats; handle encrypted, malformed, huge, and scanned documents safely.
- [ ] Add image understanding beyond OCR through an explicitly selected image-sharing/upload flow; show exactly what leaves the device.
- [ ] Implement visible accessibility workflows with per-action confirmation and safe failure handling.
- [ ] Use official Android intents/share contracts for WhatsApp and Instagram where available. Do not claim access to private app databases or hidden inboxes.
- [ ] Add an audit log for granted permissions, external transmissions, and sensitive actions.

### 4. Memory, learning, and privacy
- [ ] Define memory categories, retention, source/provenance, confidence, correction, export, and deletion flows.
- [ ] Learn only from explicit user-approved feedback/preferences; exclude credentials, secrets, and sensitive data by default.
- [ ] Encrypt local durable memory and verify key invalidation, backup/restore, migration, and corruption recovery.
- [ ] Add controls to inspect, correct, export, pause, and clear JARVIS memory.

### 5. Intelligence, connectivity, and recovery
- [ ] Add provider health checks, bounded timeouts, retry/backoff, circuit breaker, and clear offline/degraded modes.
- [ ] Add a local model/runtime only where it fits the target device's storage, RAM, thermal, and battery budget.
- [ ] Make web research/tool use source-grounded; preserve citations and distinguish verified facts from assumptions.
- [ ] Add self-diagnostics and safe repair proposals. JARVIS must not silently modify its own code, permissions, security settings, or external accounts.
- [ ] Add tests for prompt injection in retrieved documents, untrusted tool output, malformed plans, duplicate actions, and replayed approvals.

### 6. Product quality and release
- [ ] Finish premium UI, accessibility, responsive layouts, loading/error/empty states, and live task status.
- [ ] Verify copy, voice, settings, updater, gateway configuration, and permission onboarding end-to-end.
- [ ] Add unit, integration, instrumentation, and physical-device test evidence.
- [ ] Run static analysis, dependency/license review, secret scan, and release bundle inspection.
- [ ] Configure a stable user-controlled release signing key through GitHub Secrets; never commit keystore material or request passwords in chat.
- [ ] Produce one signed release APK only after all release blockers are closed; verify signature, package ID, version, SHA-256, install/update path, and artifact integrity.
- [ ] Preserve the user's existing app data and provide a safe migration path before replacing any installed package.

## Definition of done
A feature is complete only when its implementation is merged, tests pass, its permission/privacy behavior is documented, and it has been exercised on a representative Android device. A CI debug APK is a test artifact, not a customer release. Do not create routine APK artifacts while working through this checklist.


### 7. Side-income agent (required master subsystem)
- [ ] Implement the requirements in [SIDE_INCOME_AGENT_REQUIREMENTS.md](SIDE_INCOME_AGENT_REQUIREMENTS.md): Pakistan-focused discovery of surveys, research studies, app testing, AI/data tasks, transcription, web research, and other legitimate micro-work.
- [ ] Verify each platform's current Pakistan eligibility, automation rules, and actual payout options before recommending or executing tasks.
- [ ] Support the user's available payout routes (JazzCash, Easypaisa, Payoneer, Pakistani bank account) where each platform offers them; do not imply universal platform support.
- [ ] Add opportunity ranking by expected net hourly return, reliability, eligibility, risk, withdrawal threshold, fees, and payment timing.
- [ ] Implement permitted task execution, with user handoff for genuine opinions, identity checks, CAPTCHA, physical tasks, and personal attestations.
- [ ] Add approval gates for account creation, identity submission, applications/messages, contracts, and payout changes.
- [ ] Add an auditable earnings ledger distinguishing submitted, pending, approved, rejected, and actually paid amounts.
- [ ] Test safe background scheduling, pause/revoke controls, recovery, and duplicate-submission prevention.
