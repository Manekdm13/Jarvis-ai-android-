# JARVIS online AI gateway

This Cloudflare Worker keeps provider API keys and the app's shared gateway token off the Android APK and GitHub. Android sends chat text over HTTPS; the Worker routes ordinary conversation to Gemini first and technical prompts to OpenRouter's free-model router first, with one provider fallback.

## Deploy from a computer

1. Install Node.js 20 or newer.
2. From the repository root:
   ```sh
   cd worker
   npm install
   npx wrangler login
   npx wrangler deploy
   ```
3. Add secrets at the Worker prompt (never commit or paste them into ChatGPT):
   ```sh
   npx wrangler secret put GEMINI_API_KEY
   npx wrangler secret put OPENROUTER_API_KEY
   npx wrangler secret put JARVIS_ACCESS_TOKEN
   ```
   Generate a unique gateway token of at least 32 characters, for example with Node.js:
   ```sh
   node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
   ```
   Use that same generated token for the Worker secret and the **Private gateway access token** field in the Android app. Do not reuse your Gemini API key as the gateway token.
4. Confirm the Worker has the `CHAT_LIMITER` rate-limit binding from `wrangler.toml`; if Cloudflare asks to provision it, follow the dashboard prompt and redeploy.
5. Open `https://YOUR-WORKER.workers.dev/health`. It should report `ok: true` and `authenticationConfigured: true`; provider flags show whether provider secrets exist, never their values.
6. In JARVIS, enter the Worker base URL and the generated gateway token. The app adds `/v1/chat` automatically.

## Free-first provider setup

- `GEMINI_API_KEY`: Google AI Studio key. Default model: `gemini-3.8-flash`.
- `OPENROUTER_API_KEY`: OpenRouter key. Default model: `openrouter/free`, which dynamically selects from currently available free models.
- Technical prompts try OpenRouter first; general chat tries Gemini first. If the first configured provider fails, the other is tried once.
- The gateway does not silently select paid models. Free model availability and quotas are controlled by the providers and can change.
- Gemini's free tier may use submitted prompts and responses to improve Google products. Do not send passwords, authentication codes, financial details, or other sensitive content.

## Security

- The Worker rejects every chat request without a matching bearer token of at least 32 characters.
- The gateway token is stored on Android using Android Keystore-backed AES-GCM encryption.
- Provider API keys remain Cloudflare Worker secrets and are never sent to Android.
- CORS explicitly allows the Authorization header; the endpoint does not use cookie authentication.
- The Worker checks the actual request body size, not only the optional Content-Length header, and rate-limits authenticated chat requests using Cloudflare's stable `ratelimits` binding.
- Phone actions remain local to Android and require user-visible approval. The Worker cannot operate the phone.
- The health endpoint reveals only configuration status, never secret values.
- Treat conversation content sent to an online model as data leaving the device. Do not send secrets.

## API

`GET /health` returns gateway status and boolean configuration flags.

`POST /v1/chat` requires `Authorization: Bearer YOUR_JARVIS_ACCESS_TOKEN` and a JSON body:
```json
{ "message": "Hello JARVIS", "history": "Recent local conversation text" }
```

Successful responses include `reply`, `provider`, `model`, and the routing strategy. Authentication failures return HTTP 401.
