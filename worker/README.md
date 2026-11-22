# JARVIS online AI gateway

This Cloudflare Worker keeps provider API keys and the app's shared gateway token off the Android APK and GitHub. Android sends chat text over HTTPS; the Worker authenticates the app, applies rate limits, and routes requests to configured online providers.

## Current provider routing

The provider order is task-aware. Unconfigured providers are skipped, and the Worker tries the remaining configured providers in order:

- General chat: Gemini → Groq → Cloudflare Workers AI → OpenRouter.
- Technical/coding prompts: Groq → OpenRouter → Gemini → Cloudflare Workers AI.

Current default model identifiers are configured in `wrangler.toml`:

- Gemini: `gemini-3.1-flash-lite` (Worker secret: `JARVIS_AI_KEY`).
- Groq: `openai/gpt-oss-20b` (optional Worker secret: `GROQ_API_KEY`).
- OpenRouter: `openrouter/free` (optional Worker secret: `OPENROUTER_API_KEY`).
- Cloudflare Workers AI: `@cf/meta/llama-3.1-8b-instruct` using the `AI` binding.

Provider availability, rate limits, quotas, and pricing depend on the provider account and can change. Check each provider's current dashboard before relying on a free quota or enabling a provider that may incur charges.

**Important:** Cloudflare Workers AI is still an online service. It is not an offline/on-device fallback. JARVIS does not yet include a local language model.

**xAI/Grok is not currently implemented in this Worker.** Creating an `XAI_API_KEY` secret alone will not make Grok available; provider code, routing, health reporting, and tests must be added first.

## First connection: Gemini

1. In Google AI Studio, create or copy your Gemini API key. Never paste it into the Android app, GitHub, or ChatGPT.
2. From the repository's `worker` directory, install dependencies and authenticate Wrangler:
   ```sh
   npm install
   npx wrangler login
   ```
3. Add the Gemini key as a Cloudflare Worker secret:
   ```sh
   npx wrangler secret put JARVIS_AI_KEY
   ```
   Paste the key only into the secure Wrangler prompt.
4. Generate a separate random app token of at least 32 characters:
   ```sh
   node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
   ```
   Save it privately. Add it to the Worker:
   ```sh
   npx wrangler secret put JARVIS_APP_TOKEN
   ```
   Enter the same generated app token in JARVIS's **Private gateway access token** field. Never reuse the Gemini API key as this token.
5. Deploy the Worker:
   ```sh
   npx wrangler deploy
   ```
6. Open `https://YOUR-WORKER.workers.dev/health`. Expect `ok: true`, `providers.gemini: true`, and `authenticationConfigured: true`. These are configuration flags only; the endpoint never returns secret values.
7. In JARVIS, enter the Worker base URL and generated app token. The Android client appends `/v1/chat` automatically. Send a short test message.

## Optional provider secrets

Only configure providers you intend to use:

```sh
npx wrangler secret put GROQ_API_KEY
npx wrangler secret put OPENROUTER_API_KEY
```

The model names can be overridden using the corresponding non-secret variables in `wrangler.toml`: `GEMINI_MODEL`, `GROQ_MODEL`, `OPENROUTER_MODEL`, and `WORKERS_AI_MODEL`. The Cloudflare `AI` binding is declared in `wrangler.toml`; it does not require an API key in the Android app.

## Security and limits

- The Worker rejects chat requests without a matching bearer token of at least 32 characters.
- The app token is stored on Android using Android Keystore-backed AES-GCM encryption.
- Provider API keys remain Cloudflare Worker secrets and are never sent to Android.
- The Worker rate-limits authenticated chat requests using Cloudflare's `CHAT_LIMITER` binding and checks request size.
- The health endpoint reveals configuration status only, never secret values.
- Phone actions remain local to Android and require user-visible approval. The Worker cannot operate the phone.
- Treat conversation content sent to an online model as data leaving the device. Do not send passwords, OTPs, authentication tokens, or other secrets.

## API

`GET /health` returns gateway status and boolean provider/authentication configuration flags.

`POST /v1/chat` requires `Authorization: Bearer YOUR_JARVIS_APP_TOKEN` and a JSON body:

```json
{ "message": "Hello JARVIS", "history": "Recent local conversation text" }
```

Successful responses include `reply`, `provider`, `model`, and the routing strategy. Authentication failures return HTTP 401. If all configured providers fail, the Worker returns a provider error summary without exposing API key values.

## Academic research search (new)

JARVIS also exposes an authenticated scholarly search endpoint that queries two public research indexes in parallel:

- **Semantic Scholar Academic Graph API** for paper titles, abstracts, authors, venues, DOI identifiers, citation counts, paper pages and open-access PDF links.
- **OpenAlex Works API** for an independent index, publication metadata, author lists, citation counts and open-access locations.

No paid API key is required for the default integration. Semantic Scholar's optional API key can be added as the Worker secret `SEMANTIC_SCHOLAR_API_KEY` if you obtain one and need higher/steadier quota. OpenAlex works with keyless basic access; its free daily budget and rate limits still apply. Do not add a paid OpenAlex plan or billing unless the owner explicitly chooses to.

### Android usage

In the JARVIS chat, use one of these explicit commands:

- `Research food safety in dairy processing`
- `Find papers on antimicrobial packaging`
- `Search papers for whey protein applications`

The app sends the query to the Worker over HTTPS. The Worker authenticates the request with the same private JARVIS app token, rate-limits it, queries both sources, merges and deduplicates results, and returns up to 12 records. JARVIS formats authors, year, journal/source, DOI, paper page and open-access URL when available.

### Research API

`POST /v1/research/search` requires `Authorization: Bearer YOUR_JARVIS_APP_TOKEN`:

```json
{ "query": "food safety in dairy processing", "limit": 5 }
```

The response contains `query`, `papers`, `sources`, `partial`, and `warnings`. Each paper includes its source database and available metadata. If one database is unavailable, JARVIS can still return results from the other and labels the result as partial.

This is literature discovery, not a substitute for reading the paper. Citation counts are not quality scores; verify publication status, methods, DOI and full text before relying on a paper. Abstracts and metadata may be incomplete or unavailable. The search does not download or store full papers automatically.
