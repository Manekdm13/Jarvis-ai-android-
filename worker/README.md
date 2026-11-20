# JARVIS online AI gateway

This Worker keeps provider API keys off the Android APK and GitHub. The Android app sends chat text to this gateway; the gateway calls Gemini first and OpenRouter's free-model router only if Gemini is unavailable.

## Free-first provider setup

1. Create a Worker in Cloudflare and deploy this folder with Wrangler, or import this repository and set the Worker root directory to `worker`.
2. Add Worker secrets (never commit them):
   - `GEMINI_API_KEY`: Google AI Studio key. Gemini 3.8 Flash is the default model.
   - `OPENROUTER_API_KEY`: optional backup key. The default backup model is `openrouter/free`.
3. Keep `GEMINI_MODEL` and `OPENROUTER_MODEL` as configured in `wrangler.toml`, or override them as Worker variables.
4. Test `https://YOUR-WORKER.workers.dev/health`. It should return `ok: true` and show which providers are configured.
5. In JARVIS, paste the Worker base URL (for example `https://jarvis-ai-gateway.YOUR-SUBDOMAIN.workers.dev`) into **AI gateway URL**. The app adds `/v1/chat` automatically.

Gemini's free tier may use submitted prompts and responses to improve Google products. Do not send passwords, authentication codes, financial details, or other sensitive content. The app sends recent chat text to the configured provider when you use online chat.

## Cost and limits

- The app and Worker do not silently switch to paid models.
- Gemini is attempted first; OpenRouter's `openrouter/free` is the fallback. Free availability and quotas are provider-controlled and can change.
- Groq is intentionally not enabled in the zero-cost default: its current GPT-OSS hosted models are priced per token, so enabling them could create charges.
- The Worker includes a 20-request-per-minute per-IP rate-limit binding. Cloudflare may apply additional account/platform limits.

## API

`GET /health` returns gateway status (never exposes secrets).

`POST /v1/chat` body:
```json
{ "message": "Hello JARVIS", "history": "Recent local conversation text" }
```

The response contains `reply`, `provider`, and `model`. Provider errors never expose API keys.

## Security notes

- Never put provider keys in Android source, app resources, GitHub Actions variables, or chat messages.
- The Android client only sends chat text and recent conversation text; it does not upload the encrypted local memory file.
- Phone actions are not executed by this Worker. The Android app remains responsible for user-visible, per-action approval.
- This is a starter gateway, not an authenticated multi-user service. Treat its URL as shareable and monitor usage. For a public production release, add user authentication and stronger abuse controls before distributing widely.
