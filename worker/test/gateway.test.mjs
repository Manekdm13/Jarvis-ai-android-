import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";

const token = "a".repeat(64);
const limiter = { limit: async () => ({ success: true }) };
const env = (extra = {}) => ({
  JARVIS_APP_TOKEN: token,
  JARVIS_AI_KEY: "test-gemini-key",
  OPENROUTER_API_KEY: "test-openrouter-key",
  CHAT_LIMITER: limiter,
  ...extra,
});
const request = (body, bearer = token) => new Request("https://jarvis.example/v1/chat", {
  method: "POST",
  headers: { "Content-Type": "application/json", ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}) },
  body: JSON.stringify(body),
});
const response = (data, status = 200) => new Response(JSON.stringify(data), { status });

test("health exposes configuration flags but no secrets", async () => {
  const res = await worker.fetch(new Request("https://jarvis.example/health"), env());
  const data = await res.json();
  assert.equal(res.status, 200);
  assert.equal(data.authenticationConfigured, true);
  assert.equal(JSON.stringify(data).includes(token), false);
});

test("missing and incorrect bearer tokens are rejected before provider calls", async () => {
  const original = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => { calls++; return response({}); };
  try {
    for (const bearer of [null, "wrong-token"]) {
      const res = await worker.fetch(request({ message: "hello" }, bearer), env());
      assert.equal(res.status, 401);
    }
    assert.equal(calls, 0);
  } finally { globalThis.fetch = original; }
});

test("authenticated chat returns a Gemini response and allows Authorization in CORS", async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async () => response({ candidates: [{ content: { parts: [{ text: "Hello" }] } }] });
  try {
    const res = await worker.fetch(request({ message: "hello" }), env());
    const data = await res.json();
    assert.equal(res.status, 200);
    assert.equal(data.reply, "Hello");
    assert.equal(data.provider, "gemini");
    assert.equal(res.headers.get("Access-Control-Allow-Headers"), "Content-Type, Authorization");
  } finally { globalThis.fetch = original; }
});

test("Gemini-only configuration works without an OpenRouter key", async () => {
  const original = globalThis.fetch;
  let calledUrl = "";
  let calledHeaders;
  globalThis.fetch = async (url, init) => {
    calledUrl = String(url);
    calledHeaders = new Headers(init?.headers);
    return response({ candidates: [{ content: { parts: [{ text: "Gemini only" }] } }] });
  };
  try {
    const res = await worker.fetch(
      request({ message: "hello" }),
      env({ OPENROUTER_API_KEY: undefined, JARVIS_AI_KEY: "  \"test-gemini-\r\nkey\"  " }),
    );
    const data = await res.json();
    assert.equal(res.status, 200);
    assert.equal(data.reply, "Gemini only");
    assert.equal(data.provider, "gemini");
    assert.match(calledUrl, /models\/gemini-3\.8-flash:generateContent$/);
    assert.equal(calledHeaders.get("x-goog-api-key"), "test-gemini-key");
  } finally { globalThis.fetch = original; }
});

test("OpenRouter is tried when Gemini fails", async () => {
  const original = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => ++calls === 1
    ? response({ error: "temporary" }, 503)
    : response({ model: "free-test", choices: [{ message: { content: "Fallback" } }] });
  try {
    const res = await worker.fetch(request({ message: "hello" }), env());
    const data = await res.json();
    assert.equal(res.status, 200);
    assert.equal(data.reply, "Fallback");
    assert.equal(data.provider, "openrouter");
    assert.equal(calls, 2);
  } finally { globalThis.fetch = original; }
});

test("actual oversized request bodies are rejected", async () => {
  const res = await worker.fetch(request({ message: "x".repeat(24_100) }), env());
  assert.equal(res.status, 413);
});

test("chat fails clearly when no provider is configured", async () => {
  const res = await worker.fetch(request({ message: "hello" }), env({
    JARVIS_AI_KEY: undefined, OPENROUTER_API_KEY: undefined,
  }));
  assert.equal(res.status, 503);
});

test("preflight allows the Authorization header", async () => {
  const res = await worker.fetch(new Request("https://jarvis.example/v1/chat", { method: "OPTIONS" }), env());
  assert.equal(res.status, 204);
  assert.equal(res.headers.get("Access-Control-Allow-Headers"), "Content-Type, Authorization");
});

test("Cloudflare rate-limit binding receives the documented key object", async () => {
  let received;
  const res = await worker.fetch(
    request({ message: "hello" }),
    env({ CHAT_LIMITER: { limit: async (options) => { received = options; return { success: false }; } } }),
  );
  assert.deepEqual(received, { key: token });
  assert.equal(res.status, 429);
});
