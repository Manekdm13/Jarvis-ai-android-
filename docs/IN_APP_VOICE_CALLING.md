# JARVIS in-app voice calling

## Target
JARVIS requests the owner's attention from a running background task. The task stores a checkpoint and concise question; Android receives an incoming-call notification; the owner answers a full-duplex internet voice session; the transcript is attached to the same task; the task resumes from its checkpoint.

This is app-to-app VoIP. It does not dial a SIM number and does not require Twilio/PSTN.

## Architecture
- Android: native incoming-call screen, foreground call service, microphone permission, notification channel, call history, mute/speaker/hang-up controls.
- Signalling/state: Supabase Auth + Realtime/Postgres. WebRTC offer/answer/ICE events are private to the authenticated owner.
- Media: WebRTC peer connection with STUN; configurable TURN fallback is required for restrictive mobile networks. P2P avoids PSTN minutes, but TURN bandwidth/hosting may incur cost.
- Wake-up: Firebase Cloud Messaging high-priority data notification. FCM wakes/rings the app; it does not carry audio.
- Agent: Cloudflare Worker creates an interaction request, waits for the owner's answer/transcript, and resumes the task using its saved checkpoint.
- Voice AI: existing JARVIS gateway/provider, subject to provider availability and usage limits.

## Security
- Never put Supabase service-role keys, provider keys, or FCM server credentials in the APK.
- Authenticate each device/user; enforce owner-scoped RLS on all call and task-interaction rows.
- Validate call ownership and task ownership server-side before accepting signalling events.
- Do not auto-answer. Show an explicit incoming-call notification and answer control.
- Keep microphone capture limited to an active, accepted call; show Android's microphone indicator.
- Consequential actions require separate confirmation; call audio alone cannot authorize sending, submitting, paying, deleting, or changing security settings.
- Persist task checkpoint before ringing. If missed/declined, keep task paused or use a safe timeout policy; never guess the user's answer.

## Verified repository prerequisites
- Current Android manifest has INTERNET only; no RECORD_AUDIO, POST_NOTIFICATIONS, foreground-service, or FCM service declarations yet.
- Current Gradle file has no WebRTC or Firebase Messaging SDK.
- No Firebase Android configuration file was verified in this branch.
- Connected Supabase account currently returns no projects, so the migration cannot be deployed yet.
- This design and migration are not a working calling feature. Physical-device tests are required on the Redmi Note 13 4G / Android 15.

## Build sequence
1. Connect the owner's Supabase project and apply supabase/migrations/202611280001_jarvis_voice_calls.sql.
2. Configure the Firebase Android app + FCM and add its generated google-services.json through a secure repository workflow. Keep server credentials in secrets.
3. Implement authenticated signalling, WebRTC media, incoming-call notification/foreground service, and call UI.
4. Add Cloudflare Worker endpoints for signed call invites and task checkpoint/resume; keep server credentials only as Worker secrets.
5. Add tests for background notification, accept/decline, audio route, network loss, task resume, and owner isolation; install and test on the physical device.

## Cost
The Android/WebRTC/FCM/Supabase software path can use free tiers subject to quotas and terms. Direct peer-to-peer media has no PSTN minutes. TURN relay, backend hosting, and AI voice inference may incur charges or hit free-tier limits.