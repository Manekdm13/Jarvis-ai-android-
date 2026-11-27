# JARVIS online AI gateway

This Cloudflare Worker keeps provider API keys and the app's shared gateway token off the Android APK and GitHub. Android sends chat text over HTTPS; the Worker authenticates the app, applies rate limits, and routes requests to configured online providers.

## Current provider routing

The provider order is task-aware. Unconfigured providers are skipped, and the Worker tries the remaining configured providers in order:

- General chat: Gemini → Groq → Cloudflare Workers AI → OpenRouter.
- Technical/coding prompts: Groq → OpenRouter → Gemini → Cloudflare Workers AI.

Current default model identifiers are configured in `wrangler.toml`:

- Gemini: `gemini-3.1-flash-lite` (Worker secret: `JARVIS_AI_KEY`; `GEMINI_API_KEY` is also accepted as an alias).
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
   Paste the key only into the secure Wrangler prompt. The Worker also accepts the standard name `GEMINI_API_KEY`; if you already saved the key under that name, you do not need to add it again.
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

## Cloudflare Browser Run web research (default)

The authenticated `POST /v1/web-agent` endpoint uses Cloudflare Browser Run when the Worker has its `BROWSER` binding. This avoids a separate TinyFish API key and keeps browser execution inside the Cloudflare account. The current fallback is deliberately **read-only**: it opens a public website, extracts its title, readable text, and links, then uses the configured JARVIS model provider to answer the user's research goal. It does not click buttons, submit forms, sign in, purchase items, or change website data.

The binding is configured in `wrangler.toml`:

```toml
[browser]
binding = "BROWSER"
```

The Worker uses `@cloudflare/puppeteer` to connect to Browser Run. Cloudflare documents Browser Run's free included usage and paid overage; check the [current Browser Run pricing](https://developers.cloudflare.com/browser-run/pricing/) before increasing usage. See the [Cloudflare Browser Run integration guide](https://developers.cloudflare.com/browser-run/) for limits and setup.

Health reports `providers.cloudflare_browser` as a boolean binding-availability flag. It does not expose credentials. The endpoint continues to require the same JARVIS bearer token and rate limiter as chat.

## Optional TinyFish browser automation (legacy fallback)

JARVIS exposes an authenticated browser-agent endpoint through the same Cloudflare Worker. The Android app sends a starting URL and a user-written task; the Worker forwards it to TinyFish. The TinyFish API key is never included in the APK or returned to the app.

1. Create or copy a TinyFish API key from the TinyFish API keys page.
2. Add it to the Cloudflare Worker as a secret (from the `worker` directory):
   ```sh
   npx wrangler secret put TINYFISH_API_KEY
   ```
3. Deploy the Worker after adding the secret:
   ```sh
   npx wrangler deploy
   ```
4. Open JARVIS → **TinyFish Web Agent**, enter a starting website URL and a clear task, then run it. The result is added to the conversation.

API: `POST /v1/web-agent`, authenticated with the same `Authorization: Bearer YOUR_JARVIS_APP_TOKEN` header as chat. JSON body:

```json
{ "url": "https://example.com", "goal": "Summarize the public pricing plans. Do not purchase anything." }
```

The Worker calls TinyFish's synchronous automation endpoint and returns the provider result. Browser-agent steps are metered against the TinyFish wallet (current account rates are shown in the TinyFish dashboard; the connected account currently reports $0.016 per step). Search/fetch may have different rates. Use specific, read-only goals when possible. Do not ask the agent to submit payments, purchase items, send messages, delete data, or change account/security settings without reviewing the exact action and obtaining fresh user confirmation.

The Worker health endpoint reports only whether the TinyFish secret is configured; it never exposes the key. If the secret is missing, `/v1/web-agent` returns HTTP 503 with setup guidance.

### Browser form automation

The same authenticated `POST /v1/web-agent` endpoint accepts an optional `mode: "automation"` and `context` field. Automation mode deliberately uses the server-side TinyFish browser agent even when Cloudflare Browser Run is configured for read-only research. The Worker adds guardrails: it may fill matching fields from the supplied owner-approved profile, must not invent missing facts, and must stop before final Submit/Apply/Send/Pay/Delete actions. It must stop for passwords, OTPs, CAPTCHA, recovery codes, payment data, or account/security verification.

Example request:

```json
{
  "url": "https://university.example/application",
  "goal": "Complete the scholarship form using my saved profile and save a draft. Do not submit.",
  "mode": "automation",
  "context": "Owner-approved profile fields..."
}
```

The Android app can queue this browser task through WorkManager, allowing the server-side browser session to run after the app is closed, subject to Android background scheduling and network availability. This requires the Cloudflare Worker secret `TINYFISH_API_KEY`; the Cloudflare Browser Run binding alone remains read-only. The task result is a draft/progress report, not proof that every field was accepted by the university portal. The owner must review the completed application and explicitly submit it.

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
