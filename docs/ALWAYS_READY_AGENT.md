# JARVIS always-ready assistant architecture

## Implemented in this milestone

- A local daily request budget of 40 AI requests, resetting by the device's local calendar day. This is a protective app-side cap, not a provider quota promise.
- A **Queue task for background** action. It submits one user-authored task to Android WorkManager.
- WorkManager persists queued work across app closure and ordinary process death, waits for network connectivity, retries transient failures, and posts a completion/failure notification when notifications are allowed.
- A retry does not consume another local daily request.
- Every queued item is one AI response request. It does not yet operate other apps, browse autonomously, or perform a chain of external actions.

## Always-on wake phrase: planned, not falsely simulated

A true "Hey JARVIS" listener requires a dedicated offline wake-word model and an Android microphone foreground service. It must:
- Be started by the user while JARVIS is visible, after the user grants RECORD_AUDIO.
- Show Android's persistent microphone foreground-service notification while listening.
- Stop listening immediately from a notification/app control.
- Run wake-word detection locally; only after the wake phrase should speech be captured and sent to the AI gateway.
- Avoid uploading ambient audio. Do not keep raw audio recordings.
- Respect Android/OEM background restrictions and battery optimization. Android may stop the service; no app can promise uninterrupted 24/7 microphone availability on every Xiaomi firmware build.
- Never silently start microphone capture on boot or bypass Android permission prompts.

The current branch deliberately does **not** claim to provide wake-word detection. A reliable offline engine (for example, a bundled Vosk/sherpa-onnx-compatible wake-word model) still needs to be selected, packaged, benchmarked on the Redmi Note 13, and integrated with an explicit foreground-service start/stop UI. Android speech recognition intents are not a substitute for a persistent offline wake-word engine.

## Autonomous thinking and task execution: planned next layer

The safe agent loop should be bounded:
1. Convert the user's goal into a short task plan and show it.
2. Identify which steps are read-only and which change data or send messages.
3. Execute only allow-listed, technically supported tools.
4. Ask for approval immediately before each sensitive or irreversible action (messages, purchases, account changes, deleting files, posting, etc.).
5. Verify the result, retry a small number of times, and report what succeeded and what did not.
6. Stop at a configurable per-task step limit and daily request budget. Never silently switch to paid providers.

An AI response alone cannot grant Android permissions or safely operate arbitrary apps. Each device capability needs its own implementation, permission disclosure, approval flow, and physical-device tests.

## Free-use expectations

- App-side cap: 40 AI calls/day in this prototype; the user can lower it in a future settings screen.
- Provider quotas and free model availability are separate and can be lower, variable, region-dependent, or temporarily unavailable.
- WorkManager is designed for deferrable background work. Android chooses execution timing; it is not a real-time 24/7 daemon.
- Background queued AI tasks require internet and a configured free-tier gateway. Offline operation is not included in this milestone.
