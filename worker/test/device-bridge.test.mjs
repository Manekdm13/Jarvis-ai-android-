import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.ts";

const deviceToken = "device-bridge-test-token-".padEnd(48, "x");
function mockDb() {
  const calls = [];
  return {
    calls,
    prepare(sql) {
      return {
        bind(...values) {
          return {
            run: async () => { calls.push({ sql, values }); return { success: true }; },
            first: async () => ({ device_id: "redmi-termux", name: "Redmi Termux" }),
            all: async () => ({ results: [{ event_type: "heartbeat" }] }),
          };
        },
      };
    },
  };
}
function env(extra = {}) {
  return { JARVIS_DEVICE_TOKEN: deviceToken, DEVICE_DB: mockDb(), ...extra };
}
function req(path, body, token = deviceToken) {
  return new Request("https://jarvis.example" + path, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: "Bearer " + token } : {}) },
    body: JSON.stringify(body),
  });
}

test("device heartbeat rejects missing or incorrect credentials", async () => {
  const db = mockDb();
  const res = await worker.fetch(req("/v1/device/heartbeat", { device_id: "redmi-termux" }, "wrong"), env({ DEVICE_DB: db }));
  assert.equal(res.status, 401);
  assert.equal(db.calls.length, 0);
});

test("device heartbeat records device metadata without storing the bearer token", async () => {
  const db = mockDb();
  const res = await worker.fetch(req("/v1/device/heartbeat", {
    device_id: "redmi-termux", name: "Redmi Note 13", agent_version: "0.1.0",
    branch: "feature/jarvis-3-0-21-sections", commit: "abc123",
  }), env({ DEVICE_DB: db }));
  const data = await res.json();
  assert.equal(res.status, 200);
  assert.equal(data.ok, true);
  assert.equal(data.device_id, "redmi-termux");
  assert.equal(db.calls.length, 2);
  assert.match(db.calls[0].sql, /INSERT INTO devices/);
  assert.match(db.calls[1].sql, /INSERT INTO task_events/);
  assert.equal(JSON.stringify(db.calls).includes(deviceToken), false);
});

test("device events accept only the fixed event vocabulary", async () => {
  const db = mockDb();
  const blocked = await worker.fetch(req("/v1/device/event", {
    device_id: "redmi-termux", event_type: "run_arbitrary_shell",
  }), env({ DEVICE_DB: db }));
  assert.equal(blocked.status, 400);
  assert.equal(db.calls.length, 0);
  const accepted = await worker.fetch(req("/v1/device/event", {
    device_id: "redmi-termux", event_type: "task_finished", task_id: "issue-123",
    details: { action: "worker_tests", exit_code: 0 },
  }), env({ DEVICE_DB: db }));
  assert.equal(accepted.status, 200);
  assert.equal(db.calls.length, 2);
});

test("device status returns device and recent audit events only to authenticated clients", async () => {
  const db = mockDb();
  const res = await worker.fetch(new Request("https://jarvis.example/v1/device/status?device_id=redmi-termux", {
    headers: { Authorization: "Bearer " + deviceToken },
  }), env({ DEVICE_DB: db }));
  const data = await res.json();
  assert.equal(res.status, 200);
  assert.equal(data.device.device_id, "redmi-termux");
  assert.equal(data.events[0].event_type, "heartbeat");
});
