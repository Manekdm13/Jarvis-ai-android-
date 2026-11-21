import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";

const token = "a".repeat(64);
const limiter = { limit: async () => ({ success: true }) };
const env = (extra = {}) => ({
  JARVIS_APP_TOKEN: token,
  JARVIS_AI_KEY: "test-gemini-key",
  OPENROUTER_API_KEY: "test-openrouter-key",
  GROQ_API_KEY: "test-groq-key",
  AI: { run: async () => ({ response: "Workers AI test" }) },
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
  globalThis.fetch = async () => response({ model: "gemini-3.1-flash-lite", status: "completed", steps: [{ type: "model_output", content: [{ type: "text", text: "Hello" }] }] });
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
    return response({ model: "gemini-3.1-flash-lite", status: "completed", steps: [{ type: "model_output", content: [{ type: "text", text: "Gemini only" }] }] });
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
    assert.equal(calledUrl, "https://generativelanguage.googleapis.com/v1beta/interactions");
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
    const res = await worker.fetch(request({ message: "hello" }), env({ GROQ_API_KEY: undefined, AI: undefined }));
    const data = await res.json();
    assert.equal(res.status, 200);
    assert.equal(data.reply, "Fallback");
    assert.equal(data.provider, "openrouter");
    assert.equal(calls, 2);
  } finally { globalThis.fetch = original; }
});


test("Groq provider is called through its OpenAI-compatible chat endpoint", async () => {
  const original = globalThis.fetch;
  let calledUrl = "";
  globalThis.fetch = async (url) => {
    calledUrl = String(url);
    return response({ model: "openai/gpt-oss-20b", choices: [{ message: { content: "Groq reply" } }] });
  };
  try {
    const res = await worker.fetch(request({ message: "debug this code" }), env({ JARVIS_AI_KEY: undefined, OPENROUTER_API_KEY: undefined }));
    const data = await res.json();
    assert.equal(res.status, 200);
    assert.equal(data.provider, "groq");
    assert.equal(data.reply, "Groq reply");
    assert.equal(calledUrl, "https://api.groq.com/openai/v1/chat/completions");
  } finally { globalThis.fetch = original; }
});

test("Cloudflare Workers AI is available as a provider fallback", async () => {
  const res = await worker.fetch(request({ message: "hello" }), env({
    JARVIS_AI_KEY: undefined, GROQ_API_KEY: undefined, OPENROUTER_API_KEY: undefined,
    AI: { run: async () => ({ response: "Cloudflare reply" }) },
  }));
  const data = await res.json();
  assert.equal(res.status, 200);
  assert.equal(data.provider, "workers_ai");
  assert.equal(data.reply, "Cloudflare reply");
});

test("actual oversized request bodies are rejected", async () => {
  const res = await worker.fetch(request({ message: "x".repeat(24_100) }), env());
  assert.equal(res.status, 413);
});

test("chat fails clearly when no provider is configured", async () => {
  const res = await worker.fetch(request({ message: "hello" }), env({
    JARVIS_AI_KEY: undefined, OPENROUTER_API_KEY: undefined, GROQ_API_KEY: undefined, AI: undefined,
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
