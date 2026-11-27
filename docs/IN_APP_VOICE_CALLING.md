# JARVIS in-app voice calling

## Target
JARVIS pauses a running task, persists a checkpoint and question, notifies the owner's Android app, and lets the owner answer in a two-way internet voice session. The answer transcript is attached to the same task before safe resumption.

This is app-to-app VoIP. It does not dial a SIM number and does not use Twilio/PSTN.

## Architecture
- Android: incoming-call notification/screen, foreground call service, microphone permission, call controls, mute/speaker/hang-up.
- Signalling/state: Supabase Auth + Realtime/Postgres. WebRTC offer/answer/ICE events are owner-scoped.
- Media: WebRTC peer connection. Start with free STUN/P2P; add a TURN relay only if testing proves it is necessary and a genuinely free provider/hosting option is available. Calls may fail on restrictive carrier/NAT networks without TURN.
- Wake-up: Firebase Cloud Messaging high-priority data notification. FCM is no-cost on Firebase Spark; it wakes/rings the app but does not carry audio.
- Agent: Cloudflare Worker creates an interaction request, waits for the owner's answer/transcript, and resumes from the saved checkpoint.
- Voice AI: existing JARVIS gateway/provider, subject to provider free-tier quotas and availability.

## Security
- Never put Supabase service-role keys, provider keys, or FCM server credentials in the APK.
- Authenticate each device/user; enforce owner-scoped RLS on all call and task-interaction rows.
- Validate call ownership and task ownership server-side before accepting signalling events.
- Do not auto-answer. Show an explicit incoming-call notification and answer control.
- Keep microphone capture limited to an active, accepted call; show Android's microphone indicator.
- Consequential actions require separate confirmation; call audio alone cannot authorize sending, submitting, paying, deleting, or changing security settings.
- Persist task checkpoint before ringing. If missed/declined, keep task paused or use a safe timeout policy; never guess the user's answer.

## Backend deployed
- Supabase project: `jarvis-voice-calls`
- Project ref: `felwloanguhybycvgcit`
- Region: `ap-south-1` (Mumbai)
- Project creation quote: `$0/month`
- Migration `202611280001_jarvis_voice_calling` applied successfully.
- Verified tables: `jarvis_calls`, `jarvis_task_interactions`, `jarvis_call_events`; RLS enabled on all three.
- Security advisor: no findings at verification time.
- Performance advisor identified missing covering indexes; owner/call indexes were added afterward.
- Do not commit service-role credentials. Client integration must use a publishable key and authenticated sessions.

## Remaining integration prerequisites
- The Android repository currently has no Firebase Messaging SDK/config, no WebRTC SDK, no Supabase Auth client, and no call service/UI.
- A Firebase Android app must be registered for application ID `com.manekdm13.jarvis` and its `google-services.json` supplied through a secure workflow. Keep Firebase server credentials in secrets. Firebase Cloud Messaging is no-cost on Spark; no billing account is needed for FCM.
- Add Supabase Auth sign-in/device registration before exposing call rows to the app. The RLS policies intentionally deny anonymous unauthenticated access.
- Implement authenticated signalling, WebRTC media, FCM incoming notification/foreground service, call UI, and Worker endpoints for signed invites/task checkpoint/resume.
- Physical-device tests are required on Redmi Note 13 4G / Android 15. Do not claim background ringing or live calling until these pass.

## Build sequence
1. Configure Firebase project/app + FCM and provide the generated Android config securely.
2. Add Supabase Auth session handling and register the device's FCM token under the authenticated owner.
3. Implement WebRTC signalling/media and incoming-call foreground service.
4. Add Cloudflare Worker endpoints for signed call invites and task checkpoint/resume; keep server credentials only as Worker secrets.
5. Test background notification, accept/decline, audio route, network loss, task resume, and owner isolation on the physical device.

## Cost policy
Use Firebase Spark FCM (no-cost), Supabase only while its free project allowance remains active, free STUN/P2P first, and existing free-tier gateway/provider options. Never enable paid TURN, PSTN, phone verification, paid AI, or billing upgrades without explicit approval. P2P calling is not guaranteed across every mobile network without a relay; if a relay is needed and no free option is reliable, disclose that limitation instead of silently adding a paid service.
