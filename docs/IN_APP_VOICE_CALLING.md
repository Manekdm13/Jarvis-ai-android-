# JARVIS in-app voice calling

## Target
JARVIS pauses a running task, persists a checkpoint and question, notifies the owner's Android app, and lets the owner answer in a two-way internet voice session. The answer transcript is attached to the same task before safe resumption.

This is app-to-app VoIP. It does not dial a SIM number and does not use Twilio/PSTN.

## Architecture
- Android: incoming-call notification/screen, foreground call service, microphone permission, call controls, mute/speaker/hang-up.
- Signalling/state: Supabase Auth + Realtime/Postgres. WebRTC offer/answer/ICE events are owner-scoped.
- Media: WebRTC peer connection. Start with free STUN/P2P; add a TURN relay only if testing proves it is necessary and a genuinely free provider/hosting option is available. Calls may fail on restrictive carrier/NAT networks without TURN.
- Wake-up: Firebase Cloud Messaging high-priority data notification. FCM wakes/rings the app but does not carry audio.
- Agent: authenticated Supabase Edge Function creates an interaction request and persists a task checkpoint; the task answer is attached to that same interaction before the caller resumes it.
- Voice AI: existing JARVIS gateway/provider, subject to provider free-tier quotas and availability.

## Security
- Never put Supabase service-role keys, provider keys, or FCM server credentials in the APK.
- Authenticate each device/user; enforce owner-scoped RLS on all call, device and task-interaction rows.
- Validate call ownership server-side before accepting signalling events.
- Do not auto-answer. Show an explicit incoming-call notification and answer control.
- Keep microphone capture limited to an active, accepted call; show Android's microphone indicator.
- Consequential actions require separate confirmation; call audio alone cannot authorize sending, submitting, paying, deleting, or changing security settings.
- Persist task checkpoint before ringing. If missed/declined, keep task paused or use a safe timeout policy; never guess the user's answer.

## Backend deployed
- Supabase project: `jarvis-voice-calls`
- Project ref: `felwloanguhybycvgcit`
- Region: `ap-south-1` (Mumbai)
- Project creation quote: `$0/month`
- Migrations `202611280001_jarvis_voice_calling` and `202611280002_jarvis_device_registry` applied.
- Tables: `jarvis_calls`, `jarvis_task_interactions`, `jarvis_call_events`, `jarvis_devices`; RLS enabled.
- Edge Function `jarvis-call-control` deployed ACTIVE with JWT verification required. Actions: register_device, disable_device, create_interaction, answer, decline, hangup, submit_answer, mark_resumed.
- The Edge Function uses the caller's authenticated JWT and anon key, not the service-role key.
- Security advisor reported no findings at last check. Owner/call indexes added after performance review.
- Keep service-role and provider credentials out of the APK and repository.

## Remaining integration prerequisites
- The Android repository still needs Firebase Messaging configuration/SDK, WebRTC SDK integration, Supabase Auth session handling, call service/UI and client-side signalling.
- A Firebase Android app must be registered for application ID `com.manekdm13.jarvis` and its `google-services.json` supplied securely. Firebase server credentials must remain in secrets.
- Register the authenticated Android device's FCM token using the Edge Function before it can receive incoming-call notifications.
- Implement authenticated WebRTC offer/answer/ICE signalling, incoming-call foreground service, call UI and the push-delivery path.
- Physical-device tests are required on Redmi Note 13 4G / Android 15. Background ringing and live calling are not implemented or verified yet.

## Build sequence
1. Configure Firebase project/app + FCM and supply generated Android config securely.
2. Add Supabase Auth session handling and register FCM token via `jarvis-call-control`.
3. Implement WebRTC signalling/media and incoming-call foreground service.
4. Connect task orchestration to create_interaction, submit_answer and mark_resumed.
5. Test background notification, accept/decline, audio route, network loss, task resume, and owner isolation on the physical device.

## Cost policy
Use Firebase Spark FCM (no-cost), Supabase only while its free project allowance remains active, free STUN/P2P first, and existing free-tier gateway/provider options. Never enable paid TURN, PSTN, phone verification, paid AI, or billing upgrades without explicit approval. P2P calling is not guaranteed across every mobile network without a relay; if a relay is needed and no free option is reliable, disclose that limitation instead of silently adding a paid service.
