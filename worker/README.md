# JARVIS online AI gateway

This Cloudflare Worker keeps provider API keys and the app's shared gateway token off the Android APK and GitHub. Android sends chat text over HTTPS; the Worker authenticates the app, applies rate limits, and routes requests to configured providers.

## Free Gemini setup (recommended first connection)

You only need a Google AI Studio API key and a separate JARVIS app token to start with Gemini. OpenRouter is optional; do not create another provider account just to get the first connection working.

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
4. Generate a separate random app token (at least 32 characters):
   ```sh
   node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
   ```
   Save the generated token privately. Add it to the Worker:
   ```sh
   npx wrangler secret put JARVIS_APP_TOKEN
   ```
   Enter the same generated app token in JARVIS's **Private gateway access token** field. Never reuse the Gemini API key as this token.
5. Deploy the Worker:
   ```sh
   npx wrangler deploy
   ```
6. Open `https://YOUR-WORKER.workers.dev/health`. Expect `ok: true`, `providers.gemini: true`, and `authenticationConfigured: true`. These are boolean status flags only; the endpoint never returns secret values.
7. In JARVIS, enter the Worker base URL and the generated app token. The Android client appends `/v1/chat` automatically. Send a short test message.

## Provider and cost behavior

- Gemini is the default general-chat provider. Default model: `gemini-3.8-flash`.
- Google's Gemini API pricing page lists a free tier for this model, subject to account-specific quotas, availability, and rate limits. Free access is not an unlimited-uptime guarantee.
- Gemini free-tier prompts and responses may be used by Google to improve its products. Do not send passwords, authentication codes, financial details, or other sensitive content.
- OpenRouter is optional. If configured, technical prompts try its `openrouter/free` model pool first; other prompts try Gemini first. A failed first provider can fall back to the other configured provider.
- The gateway does not silently select paid models. Free model availability and quotas are controlled by the providers and can change.

## Optional OpenRouter fallback

If you later want a second free-provider route, add an OpenRouter key as a Worker secret:
```sh
npx wrangler secret put OPENROUTER_API_KEY
```
The default model is `openrouter/free`. It is not required for the first Gemini-only connection.

## Security and limits

- The Worker rejects chat requests without a matching bearer token of at least 32 characters.
- The app token is stored on Android using Android Keystore-backed AES-GCM encryption.
- Provider API keys remain Cloudflare Worker secrets and are never sent to Android.
- CORS allows the `Authorization` header; the endpoint does not use cookie authentication.
- The Worker checks the actual request body size and rate-limits authenticated chat requests using Cloudflare's `CHAT_LIMITER` binding.
- Phone actions remain local to Android and require user-visible approval. The Worker cannot operate the phone.
- The health endpoint reveals only configuration status, never secret values.
- Treat conversation content sent to an online model as data leaving the device. Do not send secrets.

## API

`GET /health` returns gateway status and boolean configuration flags.

`POST /v1/chat` requires `Authorization: Bearer YOUR_JARVIS_APP_TOKEN` and a JSON body:
```json
{ "message": "Hello JARVIS", "history": "Recent local conversation text" }
```

Successful responses include `reply`, `provider`, `model`, and the routing strategy. Authentication failures return HTTP 401.


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
