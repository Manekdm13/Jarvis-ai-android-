# JARVIS hands-free voice companion

## Product behavior

The owner can enable a hands-free mode from the app. When enabled, JARVIS should remain available while the app UI is not open, subject to Android's microphone and foreground-service rules.

Expected interaction:

1. Owner says the wake phrase, such as "Jarvis".
2. JARVIS gives a short spoken acknowledgement ("I'm here" / "JARVIS awake").
3. JARVIS listens for the user's request.
4. It sends the recognized request to the configured AI gateway, using the saved conversation/profile context only when the owner has enabled that context.
5. JARVIS speaks the answer aloud, then returns to wake listening for a follow-up.
6. The owner can say "stop listening", "sleep", or use a persistent notification action to pause the microphone.

This is a spoken conversation, not a chat UI flow. No response should require opening the app or reading a message.

## Android implementation requirements

- Implement a dedicated, lifecycle-aware microphone foreground service. Start it only from a visible user action after runtime microphone permission and an explanatory consent screen.
- Declare the microphone foreground-service permissions and service type required by the target Android SDK.
- Show a persistent, truthful notification while the microphone is active, with Pause/Stop actions and a tap-through to the app.
- Use an explicit, opt-in wake-word detector (prefer a local keyword-spotting model, not repeated full speech-to-text requests). Do not stream ambient audio to the AI gateway.
- After the wake word, capture one user utterance, stop capture, and only then send the recognized text to the gateway. Play the spoken response with TextToSpeech and resume listening after speech finishes.
- Add a voice conversation state machine: Disabled, WakeListening, CapturingCommand, Thinking, Speaking, Paused, Error.
- Avoid listening while TTS is speaking; add echo handling and a timeout for no command after wake.
- Keep gateway URL/token in the existing encrypted settings. Never place provider API keys or tokens in logs, notifications, or source.
- Use the existing gateway and conversation/profile consent rules. Profile context is included only if the owner has saved/enabled it.
- Do not execute sensitive phone actions from voice alone. Reuse the existing one-time approval UI, brought to the foreground when Android permits, or ask the user to confirm in the app/notification.
- Provide controls for wake phrase, language/voice, response volume, quiet hours, pause/resume, and battery-conscious mode.

## Android platform constraints

- Android requires microphone permission and a correctly declared microphone foreground service. On modern Android versions, starting a microphone foreground service from the background is restricted; the owner must enable/start it while the app is visible.
- A persistent notification and visible microphone indicator are required while the service is active. This must never be hidden or represented as silently running.
- Battery optimization, OEM background restrictions (including HyperOS), microphone access toggles, force-stop, reboot, and speech-recognition availability can interrupt the service. Provide clear diagnostics and a manual resume path; do not promise uninterrupted availability after force-stop or every reboot.
- Continuous generic speech recognition is not a robust wake-word detector and can consume substantial battery/network. Prefer a small local keyword spotter and validate its model size, false-wake rate, latency, battery use, and offline behavior on the owner's Redmi Note 13 4G.
- TTS and recognition must support the user's selected language/installed speech services. Voice should be natural and conversational; use streaming voice service when available, with Android TTS as an offline/fallback option.

## Acceptance tests

- Wake phrase works from home screen/another app while voice mode is enabled, without opening JARVIS.
- Wake acknowledgement is spoken; command is captured without a text field.
- Answer is spoken; follow-up conversation can continue hands-free.
- Pause/stop notification and spoken stop command disable microphone capture promptly.
- Microphone indicator and foreground notification remain visible during listening.
- No ambient audio is sent before a wake event; command audio/text is sent only after activation and with disclosure.
- Gateway failure produces a spoken, concise explanation and returns to a safe listening/paused state.
- No duplicate TTS/recognition loops; service handles screen off, interruption, permission denial, and Android process recreation safely.
- Physical-device validation on Redmi Note 13 4G / HyperOS is required before calling this feature ready.

## Status

This document defines the intended feature. A passing Android build or emulator test does not mean wake-word detection or background microphone operation is implemented or validated on the owner's phone.


## Learn-and-act task engine

JARVIS should also accept open-ended requests such as "learn about X", "research X", "do this task", or "find a way to complete Y". The goal is useful execution, not a plan-only response.

### Expected behavior

- Interpret the goal, identify what success looks like, and start work immediately when the request is clear.
- Break complex work into small steps, select available tools/capabilities, and execute steps in sequence while tracking progress.
- For "learn about X", research trustworthy and relevant sources, compare conflicting information, retain a concise knowledge note with source links, and report what was learned. Separate verified facts from uncertainty.
- For "do X", perform permitted actions using available app/API/device integrations, verify the result where possible, and report completed actions versus blockers. Never claim an action succeeded without evidence.
- Continue with the next safe step instead of asking for approval at every ordinary step. Ask only when a required detail is missing, a tool is unavailable, or the next action is sensitive, costly, destructive, or externally consequential.
- When blocked, try a reasonable supported alternative, explain the limitation, and preserve completed work rather than repeatedly retrying the same failure.
- Keep tasks resumable: save goal, checklist, current step, outputs, source references, and errors so JARVIS can continue after interruption or app restart.
- Support immediate tasks and explicitly scheduled/background tasks. Show active task status, estimated/observed progress, pause/cancel controls, and a completion summary.
- Use bounded retries, timeouts, and resource limits. Avoid endless loops, duplicate actions, unnecessary research, and wasting battery/data.
- Use the owner's consent settings for memory. Do not automatically store every researched item as personal memory; save durable notes to a searchable knowledge store with sources and allow review/deletion.
- Require owner confirmation before purchases, sending messages, submitting applications/forms, deleting data, changing security settings, or other high-impact actions.

### Task completion standard

A task is complete only when JARVIS has produced the requested output or verified the requested action. If it cannot finish, it should return a concise progress report, what remains, and the exact blocker. It should not present an intention or checklist as completed work.

### Status

This is a product requirement, not yet an implemented autonomous task engine. It requires a task orchestrator, tool registry, durable task storage, source-backed knowledge retrieval, execution/verification logic, and explicit permission boundaries.
