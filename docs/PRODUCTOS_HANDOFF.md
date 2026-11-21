# ProductOS → GitHub Codespaces handoff

Last reconciled: 2026-11-21

This file carries forward the useful project context recovered from the existing ProductOS project into the existing GitHub repository. It is intended as the durable briefing for ChatGPT, GitHub coding agents, and contributors opening this repository in Codespaces.

## Canonical project and repository

- Product: JARVIS Android personal AI assistant.
- Existing GitHub repository: https://github.com/Manekdm13/Jarvis-ai-android-
- Working branch: `fix/jarvis-chat-gateway-ci`
- ProductOS project: `Jarvis-ai-android-` (ID `7b279a9a-17ef-4e2c-81b4-b972760d0e2a`).
- ProductOS metadata confirms this project is linked to the same GitHub repository and branch, with read and push access.
- Do not create a replacement repository or duplicate project.
- The GitHub Codespaces devcontainer is in `.devcontainer/devcontainer.json`; Android SDK setup is in `.devcontainer/setup-android.sh`.

## ProductOS recovery findings and limitations

The live ProductOS project context returned no wiki content. Its decision list was empty, and its ideation, research, and PRD stage boards were all empty (revision 0). ProductOS semantic project-memory search returned no matching memory entries. No project constraints were defined. A recent-activity request returned an error, so this handoff does not claim to contain inaccessible agent transcripts or all historical chat messages.

ProductOS project metadata:
- Stage: DEVELOP; source surface BUILD; project type was detected as WEB/universal even though the linked repository is a native Kotlin Android project. Treat the GitHub repository and its Gradle files as authoritative for the actual stack.
- Workspace: Personal; access level FULL_DEV.
- Deployment mode: MANUAL; ProductOS pushes do not automatically deploy.
- No database is configured; no ProductOS storage is provisioned.
- ProductOS sandbox was last booted at 2026-11-21 08:54 UTC but is currently stopped.
- ProductOS environment-variable listing exposes only the key name `CLOUDFLARE_ACCOUNT_ID`; values are encrypted and were not read or copied.

ProductOS Free usage was checked on 2026-11-21:
- 0 of 200 one-time credits remaining; the 200 credits are used and do not renew.
- Most recent attempted sandbox bash action at 10:53 UTC failed due to insufficient credits.
- Prior spend: 166.2 credits on agent runs, 32 on sandbox time, 1 on generation.
- Free plan limits reported: 2 of 3 active projects, 0 of 1 databases; publishing requires a paid plan.
- Do not rely on ProductOS sandbox execution for ongoing development. Use GitHub Codespaces and GitHub Actions instead, and keep the workflow within free allowances. Stop Codespaces when idle.

## Product vision

JARVIS is a general-purpose personal AI assistant for Android. It should handle everyday requests and specialist work across finance and markets, health, fitness, science, food technology, software engineering, education, and niche technical subjects.

It must not present model training data as live or authoritative. It should select tools according to task needs, retrieve current information when freshness matters, disclose uncertainty, cite sources, and protect user data.

## Intended architecture

1. Conversation orchestrator: classify intent, detect multi-domain requests, estimate risk and freshness requirements, choose the smallest sufficient specialist workflow/tools, and synthesize the response.
2. Core model: Gemini is the initial default provider. Keep a provider abstraction so providers can be added without rewriting Android UI code.
3. Specialist workflows: finance, health, fitness, research, food science, software/technology, education, and general assistance. These are modular workflows, not a requirement for separate paid models.
4. Live-data/research connectors: approved market data, FX, reputable health references, scientific-paper search, web search, calculators, and domain APIs. Each connector must document source, freshness timestamp, rate limits, licensing/cost, and failure behavior.
5. Personal memory: opt-in, user-visible controls to inspect, correct, export, and delete. Minimize retained data; sensitive health/financial data must not be retained without explicit opt-in.
6. Android action layer: narrow, permission-gated tools. Consequential actions require explicit user confirmation and Android permissions.

## Specialist requirements

### Finance and markets
- Explain market concepts, company fundamentals, financial statements, ratios, macroeconomic events, and portfolio risk.
- Current quotes, historical prices, FX, and market news must come from configured data providers.
- Display source, market, currency, timestamp, and delayed/live status.
- Distinguish education from personalized investment advice. Never guarantee returns or execute trades without a separately designed and explicitly authorized brokerage integration.

### Health and medicine
- Explain terminology, general health information, lab-test concepts, and reputable evidence.
- Identify emergency warning signs and direct users to urgent professional care when appropriate.
- Do not diagnose, prescribe, change medication, or replace clinicians.
- Treat uploaded health data as sensitive; no long-term memory without explicit opt-in.

### Fitness and nutrition
- Adapt exercise, recovery, and general nutrition guidance to user-stated goals and constraints.
- Track progress only with consent; distinguish estimates from measured data.
- Screen for injuries and contraindications before strenuous recommendations.

### Research and niche topics
- Prefer primary sources, scientific papers, standards, and specialist references.
- Cite real sources and publication dates; explain methods, limitations, and confidence.
- Never invent papers, citations, or specialist facts.

### Food science
- Support formulation, processing, food safety, quality systems, shelf-life study design, and nutrition calculations.
- State assumptions and cite sources.
- Add specialist capabilities through documented tool/plugin interfaces rather than growing one monolithic prompt.

### Software and cybersecurity
- Inspect connected repositories and documentation; write, test, and debug code.
- Security testing must be authorized and bounded. No unauthorized access, credential theft, stealth, persistence, or harmful exploitation.

## Routing policy

For every request:
1. Classify domain(s) and risk.
2. Decide whether current data, external research, calculations, or personal context are required.
3. Select the smallest sufficient set of tools.
4. Retrieve information and retain source metadata/timestamps for citations.
5. Synthesize a direct answer with assumptions and uncertainty.
6. Ask before storing sensitive information or taking consequential actions.

## Security and privacy requirements

- Provider API keys stay exclusively in Cloudflare Worker secrets, never in Android source, APK, Git history, or ChatGPT messages.
- Android authenticates to the Worker using a revocable app token; Android stores it encrypted using Android Keystore.
- Apply request-size limits, rate limits, timeouts, and safe errors.
- Never log API keys, access tokens, private prompts, or sensitive health/financial content.
- CORS is not authentication; restrict origins if a browser client is introduced.
- Every consequential Android action requires explicit confirmation.
- Provide clear controls for memory, permissions, and connected data sources.

## Current implementation snapshot recovered from ProductOS sandbox

The ProductOS sandbox file tree contained the same Android/Worker project shape and these important files:
- `app/src/main/java/com/manekdm13/jarvis/MainActivity.kt`
- `app/src/main/java/com/manekdm13/jarvis/AiGatewayClient.kt`
- `app/src/main/java/com/manekdm13/jarvis/ConversationMemory.kt`
- `app/src/main/java/com/manekdm13/jarvis/SecureTokenStore.kt`
- `app/src/androidTest/java/com/manekdm13/jarvis/JarvisLaunchTest.kt`
- `worker/src/index.ts`, `worker/test/gateway.test.mjs`, `worker/wrangler.toml`
- `.github/workflows/android.yml`
- `docs/JARVIS_INTELLIGENCE_ARCHITECTURE.md` and `docs/CI_DIAGNOSTICS.md`

Observed implementation details:
- Native Kotlin + Jetpack Compose Android app.
- Chat interface, Android speech-recognition intent, Text-to-Speech, and prepared phone intents with one-time explicit user approval.
- Conversation history encrypted locally using Android Keystore/AES-GCM.
- Gateway URL and app-token configuration in Android client; client sends chat over HTTPS.
- Cloudflare Worker authenticates bearer app token, applies a 20-per-60-second rate limiter, limits request body to 24 KB, and exposes a boolean-only health endpoint.
- Worker source expects `JARVIS_AI_KEY` for Gemini and optional `OPENROUTER_API_KEY` for OpenRouter. Default model strings in source/config are `gemini-3.8-flash` and `openrouter/free`.
- Worker routes technical prompts to OpenRouter first if configured; general prompts to Gemini first; falls back to the other configured provider.
- Worker returns HTTP 503 when no provider is configured and HTTP 502 when configured providers fail.
- ProductOS sandbox README is stale: it says the Worker and credentials are not connected. Cloudflare account inspection in the parallel setup established an existing `jarvis-ai-gateway` Worker and configured secret names. Never infer live health from README text; verify the deployed Worker /health endpoint and perform an authenticated chat test.
- ProductOS source snapshot's Worker CORS currently allows `*`; review before introducing browser access. The bearer token remains required for chat.
- ProductOS sandbox had a `public/develop/JARVIS-debug.apk` artifact, but its existence alone is not proof that it is current or tested. Prefer the latest GitHub Actions artifact.

## Existing Cloudflare configuration notes (names only; never copy secret values)

Previously inspected Cloudflare Worker: `jarvis-ai-gateway`, workers.dev subdomain `juttfaizan794`.
Known secret names: `GEMINI_API_KEY`, `JARVIS_AI_KEY`, `JARVIS_APP_TOKEN`. Known non-secret vars: `GEMINI_MODEL`, `OPENROUTER_MODEL`. Rate-limit binding: `CHAT_LIMITER`, namespace `1001`, 20 requests per 60 seconds.
Important source/config mismatch to keep visible:
- Worker source uses `JARVIS_AI_KEY`, not `GEMINI_API_KEY`, for Gemini.
- Worker source optionally uses `OPENROUTER_API_KEY`; that secret was not present in the inspected secret-name list. Gemini-only operation is valid; OpenRouter fallback is unavailable until explicitly configured.
- Do not rotate, overwrite, print, or commit secret values. Never paste secrets into this document.

## Delivery roadmap

### Phase 1 — reliable core
- Stabilize Android build and emulator tests.
- Verify authenticated end-to-end chat from a real device through Worker to Gemini.
- Ensure conversation history, loading/error states, and safe retries work.
- Validate provider fallback and expose provider/model metadata without secrets.

### Phase 2 — research and tool orchestration
- Add intent routing and general web/scientific research.
- Add citations, timestamps, source-quality indicators, and calculators.
- Define connector schemas and provider health checks.

### Phase 3 — specialist workflows
- Implement finance, health, fitness, food science, software, and education workflows.
- Select market-data provider only after checking coverage, licensing, latency, and cost.
- Add reputable health references and emergency escalation.
- Evaluate specialists with domain-specific and safety test cases.

### Phase 4 — personal assistant
- Add opt-in memory and user-controlled preferences.
- Add reminders, notes, calendar/task integrations, and permission-gated Android actions.
- Improve voice interaction and accessibility.

### Phase 5 — quality and expansion
- Build regression evaluations for common/niche questions, hallucination resistance, source attribution, privacy, and safety.
- Monitor latency, cost, provider outages, and answer quality.
- Add specialist domains based on actual use.

## Acceptance criteria

- Ordinary questions work without forcing users to choose a specialist.
- Current financial facts include a source and timestamp or are explicitly unavailable.
- Research answers cite retrievable sources and distinguish evidence from inference.
- Health and finance responses apply appropriate safety boundaries.
- Provider outages produce clear errors or safe fallback, never fabricated success.
- No provider secret is embedded in the APK.
- Consequential Android actions require confirmation.
- CI tests Worker logic, Android build, emulator launch, and key end-to-end flows.
- A successful CI run alone is not proof of authenticated real-device Gemini chat.

## Immediate engineering priorities

1. Use this file and the repository as the shared source of truth; ignore stale ProductOS README claims where Cloudflare has since been configured.
2. Confirm current branch, pull request, workflow run, and exact failing step before changing CI.
3. Run Worker tests, TypeScript typecheck, Wrangler dry-run, Android debug build, then emulator instrumentation; capture diagnostics on failure.
4. Verify Worker /health and then send one authenticated test message from the Android app without exposing the app token.
5. Confirm Gemini Interactions API request/response compatibility against the actual configured model and current Google documentation; do not assume the model ID or endpoint is valid solely because the code compiles.
6. Add tests for provider response parsing, timeout behavior, request validation, and provider fallback.
7. Only after the core is reliable, build the orchestrator, citations, live-data connectors, and specialist workflows incrementally.

## Sources within this repository

- `docs/JARVIS_INTELLIGENCE_ARCHITECTURE.md` — fuller product and architecture requirements.
- `docs/CI_DIAGNOSTICS.md` — CI evidence and triage workflow; update it whenever a new failure/root cause is verified.
- `worker/README.md` — gateway setup and security guidance; reconcile it with live Cloudflare state before following old setup steps.
- `README.md` — current app prototype overview.
