# JARVIS Side-Income Agent — Product and Release Requirements

Status: required master-version subsystem; specification only until implemented and tested.

## User's confirmed payout routes
The user can receive earnings through all of these routes:
- JazzCash
- Easypaisa
- Payoneer
- Pakistani bank account

This is a list of routes the user can access, NOT a claim that every earning platform supports every route. For each platform and task, JARVIS must verify the payout methods actually offered to a Pakistan-based account, minimum withdrawal, fees, currency conversion, processing time, identity requirements, and account-name rules before recommending work. Prefer the route with the best net value and practical availability. Never store banking passwords, card security codes, or OTPs.

## Mission
Find legitimate, currently accessible part-time earning opportunities for the user's location (Pakistan), help evaluate them, complete only platform-permitted work, and maintain an auditable record of work and received payments. This includes microtasks and side gigs, not only conventional freelancing:
- surveys and paid research studies;
- app, website, usability, and product testing;
- data categorization, image/audio labelling, and AI data tasks;
- transcription, proofreading, translation, and short writing;
- web research and data verification;
- eligible local task marketplaces and remote part-time work.

## Opportunity discovery and ranking
For every opportunity, capture:
- platform, official URL, task type, country/age eligibility and current registration status;
- task availability and qualification requirements;
- advertised reward, realistic estimated completion time, expected effective hourly rate, and uncertainty;
- payout routes actually confirmed for Pakistan, threshold, fees, timing, and any minimum balance;
- whether automation/AI assistance is allowed by the platform's current terms;
- identity/privacy requirements, reputation signals, and scam/risk flags;
- last-verified timestamp and evidence/source links.

Rank by estimated net earnings per hour, reliability, eligibility, payout practicality, and risk. Clearly label estimates; never guarantee task availability, acceptance, or income. Re-check changing country/payment/terms details before acting.

## Execution and consent boundaries
- Automate discovery, comparison, reminders, task organization, permitted form preparation, document drafting, time tracking, and earnings reconciliation.
- Perform a task autonomously only when the platform explicitly permits automation/AI assistance and the task does not require the user's own opinion, lived experience, identity, physical presence, biometric verification, or personal attestation.
- Surveys requiring personal opinions/experiences must be answered by the user; JARVIS may explain questions or help with accessibility, but must not fabricate answers or impersonate the user.
- Never bypass CAPTCHA, anti-bot checks, geographic restrictions, identity checks, rate limits, or platform safeguards. Never use VPN/location spoofing to evade eligibility.
- Require explicit user approval before account creation, identity-document submission, accepting contracts, sending applications/messages, submitting paid work where terms require personal attestation, or changing payout/bank details.
- Never pay deposits, unlock fees, purchase training, or buy leads without explicit informed approval; flag such offers as high risk.
- Never claim a task was completed, approved, or paid unless supported by platform/account evidence. Distinguish pending, submitted, approved, rejected, and paid.

## Earnings ledger and user controls
Provide a local durable ledger with task/platform, date, time spent, gross reward, fees, currency conversion, net amount, status, payout route, and evidence/reference. Provide weekly summaries and unpaid/rejected follow-ups. Protect account data; use least-privilege integrations and encrypted secrets. User can pause the agent, revoke platform access, inspect actions, and delete stored records.

## Android and background operation
Use permitted Android background scheduling with visible task status and notifications. Do not promise uninterrupted execution if Android force-stops the app, the device is offline, or platform controls require human interaction. Resume safely after process death and avoid duplicate submissions.

## Release acceptance tests
- Pakistan eligibility and actual payout choices are verified per platform, not inferred from the user's available methods.
- Platform automation rules are checked and enforced per task.
- Survey/personal-attestation tasks are handed to the user.
- No bypass of CAPTCHA, KYC, country restrictions, or anti-bot controls.
- Approval gates cover account registration, applications/messages, contracts, identity uploads, and payout changes.
- Ledger accurately distinguishes submitted/approved/paid and reconciles net amounts.
- Retry/recovery cannot create duplicate submissions.
- Source evidence, timestamps, privacy disclosures, and audit logs are retained.
- Physical-device tests verify background scheduling, notifications, pause/revoke controls, and safe recovery.

## Current implementation status
This file defines required behavior. It does not mean the subsystem has been implemented. The master APK must not be called complete until the code, tests, platform-policy checks, and device tests satisfy these requirements.
