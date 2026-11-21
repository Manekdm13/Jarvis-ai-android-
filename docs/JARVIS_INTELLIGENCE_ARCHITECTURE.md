# JARVIS Intelligence Architecture

## Product goal

JARVIS is a general-purpose personal AI assistant for Android. It should help with broad everyday questions and specialist work, including finance, health, fitness, science, food technology, software engineering, education, and niche technical subjects.

The product must not pretend that a language model's training data is live or authoritative. It should choose tools based on the task, retrieve current information when needed, explain uncertainty, and protect the user's data.

## Architecture

1. **Conversation orchestrator** — classifies intent, estimates risk and freshness needs, chooses a specialist workflow, and combines results.
2. **Core language model** — Gemini is the initial default provider, with a provider abstraction so additional models can be added without changing Android UI code.
3. **Specialist workflows** — modular prompt/tool configurations for finance, health, fitness, research, food science, software/technology, education, and general assistance. Specialists are workflows, not necessarily separate paid models.
4. **Live data and research tools** — approved market-data providers, currency/forex data, reputable health references, scientific-paper search, general web search, calculators, and domain APIs. Every connector must have a documented source, freshness timestamp, rate limit, and failure behavior.
5. **Personal memory** — opt-in, user-visible memory with controls to inspect, correct, export, and delete. Store the minimum necessary data; keep secrets out of conversation history.
6. **Android action layer** — a narrow, permission-gated tool interface. JARVIS may propose actions; sensitive actions require explicit user confirmation and Android permissions.

## Specialist capabilities

### Finance and markets
- Explain market concepts, company fundamentals, financial statements, ratios, macroeconomic events, and portfolio risk.
- Retrieve current quotes, historical prices, FX rates, and market news from configured data sources.
- Show source, market, currency, timestamp, and whether data is delayed.
- Clearly distinguish educational analysis from personalized investment advice. Never guarantee returns or execute trades without a separately designed, explicitly authorized brokerage integration.

### Health and medicine
- Explain medical terminology, general health information, lab-test concepts, and evidence from reputable medical references.
- Identify emergency warning signs and advise urgent professional care when appropriate.
- Do not diagnose, prescribe, change medication, or replace a clinician. Ask for missing context where it materially affects safety.
- Treat uploaded health information as sensitive; do not retain it in long-term memory unless the user explicitly opts in.

### Fitness and nutrition
- Create adaptable exercise, recovery, and general nutrition plans based on user-stated goals and constraints.
- Track progress only with consent; distinguish estimates from measured values.
- Screen for injury, contraindications, and other safety constraints before recommending strenuous activity.

### Research and niche subjects
- Search reputable primary sources, scientific papers, standards, and specialist references.
- Provide citations, publication dates, methods, limitations, and confidence.
- Say when evidence is sparse or conflicting; never invent papers, citations, or specialist facts.

### Software, technology, and cybersecurity
- Explain, write, test, and debug code; inspect repositories and documentation when connected.
- Keep security testing authorized and bounded. No unauthorized access, credential theft, stealth, persistence, or harmful exploitation.

### Food science and other domains
- Support food formulation, processing, food safety, quality systems, shelf-life study design, and nutrition calculations with assumptions and source references.
- Add new specialist workflows through a documented plugin/tool interface rather than hard-coding every niche into the base prompt.

## Routing policy

For each user request:
1. Classify the domain and whether multiple domains are involved.
2. Decide whether current data, external research, calculations, or personal context are required.
3. Select the smallest sufficient set of tools and specialist workflows.
4. Retrieve information and record source metadata and timestamps.
5. Synthesize a direct answer, cite sources where available, show assumptions and uncertainty.
6. Ask before storing sensitive personal information or taking consequential actions.

## Security and privacy requirements

- Keep provider API keys only in Cloudflare Worker secrets; never ship them in the APK or expose them to the client.
- Authenticate Android-to-Worker requests with a revocable app token; store it encrypted using Android Keystore.
- Apply request size limits, rate limits, timeouts, and safe error handling.
- Do not log access tokens, API keys, private prompts, or sensitive health/financial content.
- Restrict CORS to the actual app/web origins if a browser client is introduced; CORS is not authentication.
- Require explicit user confirmation for sending messages, sharing files, changing device settings, or any consequential action.
- Provide clear controls for memory, permissions, and connected data sources.

## Delivery roadmap

### Phase 1 — reliable core
- Stabilize Android build and emulator tests.
- Verify authenticated end-to-end chat on a real device.
- Add conversation history handling, loading/error states, and safe retries.
- Validate provider fallback and expose provider/model metadata without exposing secrets.

### Phase 2 — research and tool orchestration
- Add intent routing and a general web/scientific research tool.
- Add citations, timestamps, source-quality indicators, and calculation tools.
- Build a connector abstraction with explicit schemas and per-provider health checks.

### Phase 3 — specialist workflows
- Implement finance, health, fitness, food science, software, and education workflows.
- Add market data only after selecting a provider with clear coverage, licensing, latency, and cost.
- Add reputable health references and emergency escalation behavior.
- Evaluate each specialist with domain-specific test cases and safety tests.

### Phase 4 — personal assistant
- Add opt-in memory and user-controlled preferences.
- Add reminders, notes, calendar/task integrations, and Android actions behind permissions and confirmation.
- Add voice interaction and accessibility improvements.

### Phase 5 — quality and expansion
- Build a regression evaluation set covering common and niche questions, hallucination resistance, source attribution, privacy, and safety.
- Monitor latency, cost, provider outages, and answer quality.
- Add further specialist domains based on real user needs.

## Acceptance criteria

- A normal question is answered without forcing the user to choose a specialist.
- Current financial facts include a source and timestamp, or are clearly marked unavailable.
- Research answers cite real retrievable sources and distinguish evidence from inference.
- Health and finance answers apply appropriate safety boundaries.
- Provider outages produce clear errors or safe fallback, never fabricated success.
- No provider secret is embedded in the Android application.
- Consequential Android actions require user confirmation.
- CI tests the Worker, Android build, emulator launch, and key end-to-end chat flows.
