import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";

const token = "a".repeat(64);
const limiter = { limit: async () => ({ success: true }) };
const env = (extra = {}) => ({
  JARVIS_APP_TOKEN: token,
  GEMINI_API_KEY: "test-gemini-key",
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
    GEMINI_API_KEY: undefined, OPENROUTER_API_KEY: undefined,
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

test("Claude-only mode calls Anthropic and returns its response", async () => {
  const original = globalThis.fetch;
  let target = "";
  globalThis.fetch = async (url) => {
    target = String(url);
    return response({ model: "claude-test", content: [{ type: "text", text: "Claude reply" }] });
  };
  try {
    const res = await worker.fetch(request({ message: "hello", mode: "claude" }), env({ ANTHROPIC_API_KEY: "test-claude" }));
    const data = await res.json();
    assert.equal(res.status, 200);
    assert.equal(data.provider, "claude");
    assert.equal(data.reply, "Claude reply");
    assert.match(target, /api.anthropic.com/);
  } finally { globalThis.fetch = original; }
});

test("dual review runs OpenAI draft, Claude review, then OpenAI synthesis", async () => {
  const original = globalThis.fetch;
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push(String(url));
    if (String(url).includes("api.openai.com")) {
      const body = JSON.parse(options.body);
      return response({ model: "gpt-test", choices: [{ message: { content: body.messages[1].content.includes("Claude review") ? "Final combined answer" : "OpenAI draft" } }] });
    }
    return response({ model: "claude-test", content: [{ type: "text", text: "Claude review" }] });
  };
  try {
    const res = await worker.fetch(request({ message: "explain this", mode: "collaborate" }), env({
      OPENAI_API_KEY: "test-openai", ANTHROPIC_API_KEY: "test-claude",
    }));
    const data = await res.json();
    assert.equal(res.status, 200);
    assert.equal(data.provider, "collaboration");
    assert.deepEqual(data.providers, ["openai", "claude"]);
    assert.equal(data.reply, "Final combined answer");
    assert.equal(calls.length, 3);
    assert.match(calls[0], /api.openai.com/);
    assert.match(calls[1], /api.anthropic.com/);
    assert.match(calls[2], /api.openai.com/);
  } finally { globalThis.fetch = original; }
});

test("dual review refuses to run unless both paid-provider keys are configured", async () => {
  const res = await worker.fetch(request({ message: "hello", mode: "collaborate" }), env());
  assert.equal(res.status, 503);
});
