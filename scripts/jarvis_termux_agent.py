#!/usr/bin/env python3
"""JARVIS Termux companion agent. Executes a small, fixed set of safe project tasks."""
from __future__ import annotations

import argparse
import getpass
import json
import os
import platform
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

VERSION = "0.1.0"
REPO = "Manekdm13/Jarvis-ai-android-"
OWNER = "Manekdm13"
TASK_PREFIX = "[JARVIS TASK]"
GATEWAY = "https://jarvis-ai-gateway.juttfaizan794.workers.dev"
CONFIG_DIR = Path.home() / ".config" / "jarvis-termux-agent"
CONFIG_FILE = CONFIG_DIR / "config.json"
STATE_FILE = CONFIG_DIR / "state.json"
PROJECT = Path.home() / "Jarvis-ai-android-"
REDACT = [
    (re.compile(r"(?i)(bearer\s+)[A-Za-z0-9._~+/-]{20,}"), r"\1[REDACTED]"),
    (re.compile(r"(?i)(api[_-]?key|token|password|secret)(\s*[:=]\s*)[^\s,;]+"), r"\1\2[REDACTED]"),
]

def http_json(url: str, token: str | None = None, method: str = "GET", payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "JARVIS-Termux-Agent/" + VERSION}
    if data is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=25) as response:
        raw = response.read(1_000_000)
        return json.loads(raw.decode("utf-8")) if raw else {}

def redact(value: str) -> str:
    for pattern, replacement in REDACT:
        value = pattern.sub(replacement, value)
    return value[-3500:]

def load_config():
    if not CONFIG_FILE.exists():
        raise SystemExit("Agent is not configured. Run: python scripts/jarvis_termux_agent.py --setup")
    config = json.loads(CONFIG_FILE.read_text())
    if not config.get("token") or not config.get("device_id"):
        raise SystemExit("Agent config is incomplete. Run --setup again.")
    return config

def save_private(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")
    try:
        path.chmod(0o600)
        path.parent.chmod(0o700)
    except OSError:
        pass

def setup():
    print("JARVIS Termux Agent setup")
    print("Enter the one-time device bridge token supplied in your private ChatGPT conversation.")
    token = getpass.getpass("Device bridge token (input hidden): ").strip()
    if len(token) < 32:
        raise SystemExit("Token must be at least 32 characters.")
    device_id = "termux-" + uuid.uuid4().hex[:12]
    gateway = input("Gateway URL [press Enter for configured default]: ").strip() or GATEWAY
    if not gateway.startswith("https://"):
        raise SystemExit("Gateway URL must use HTTPS.")
    save_private(CONFIG_FILE, {"gateway": gateway.rstrip("/"), "token": token, "device_id": device_id})
    save_private(STATE_FILE, {"completed_issues": []})
    print("Saved private configuration:", CONFIG_FILE)
    print("Device ID:", device_id)
    print("The token was not displayed or written to the terminal.")

def git(*args, timeout=90):
    return subprocess.run(["git", *args], cwd=PROJECT, text=True, stdout=subprocess.PIPE,
                          stderr=subprocess.STDOUT, timeout=timeout, check=False)

def run_task(action: str):
    if action == "termux_diagnostics":
        tools = {}
        for name in ("python", "node", "npm", "git", "java", "gradle", "adb"):
            p = subprocess.run(["sh", "-lc", "command -v " + name], text=True,
                               stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
            tools[name] = p.stdout.strip() or "not installed"
        return {"ok": True, "platform": platform.platform(), "python": sys.version.split()[0],
                "project_exists": PROJECT.is_dir(), "tools": tools}
    if action == "git_status":
        if not PROJECT.is_dir():
            return {"ok": False, "error": "Project directory not found: " + str(PROJECT)}
        status = git("status", "--short", "--branch")
        head = git("log", "-1", "--format=%H %s")
        branch = git("branch", "--show-current")
        return {"ok": status.returncode == 0, "branch": branch.stdout.strip(),
                "head": head.stdout.strip(), "status": redact(status.stdout)}
    if action == "git_fetch":
        if not PROJECT.is_dir():
            return {"ok": False, "error": "Project directory not found"}
        p = git("fetch", "--prune", "origin", timeout=180)
        status = git("status", "--short", "--branch")
        return {"ok": p.returncode == 0, "fetch_output": redact(p.stdout),
                "status": redact(status.stdout), "exit_code": p.returncode}
    if action == "worker_tests":
        worker_dir = PROJECT / "worker"
        if not worker_dir.is_dir():
            return {"ok": False, "error": "worker/ directory not found"}
        results = []
        for command in (["npm", "test"], ["npm", "run", "typecheck"]):
            p = subprocess.run(command, cwd=worker_dir, text=True, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, timeout=900, check=False)
            results.append({"command": " ".join(command), "exit_code": p.returncode,
                            "output": redact(p.stdout)})
            if p.returncode:
                break
        return {"ok": all(x["exit_code"] == 0 for x in results), "steps": results}
    if action == "gradle_debug_build":
        if not PROJECT.is_dir():
            return {"ok": False, "error": "Project directory not found"}
        if not (PROJECT / "gradlew").exists():
            return {"ok": False, "error": "This repository has no gradlew wrapper. Android build on Termux is not configured; use GitHub Actions for the verified Android build."}
        p = subprocess.run(["./gradlew", "assembleDebug", "--no-daemon"], cwd=PROJECT, text=True,
                           stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=1800, check=False)
        return {"ok": p.returncode == 0, "exit_code": p.returncode, "output": redact(p.stdout)}
    return {"ok": False, "error": "Action is not allowlisted."}

def report(config, event_type, task_id=None, details=None):
    payload = {"device_id": config["device_id"], "event_type": event_type,
               "task_id": task_id, "details": details or {}}
    try:
        return http_json(config["gateway"] + "/v1/device/event", config["token"], "POST", payload)
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        # Compatibility with the currently deployed legacy Worker bridge.
        legacy = {"deviceId": config["device_id"], "eventType": event_type,
                  "taskId": task_id, "details": details or {}}
        return http_json(config["gateway"] + "/bridge/event", config["token"], "POST", legacy)

def heartbeat(config):
    payload = {"device_id": config["device_id"], "name": "Android Termux",
               "agent_version": VERSION, "platform": platform.platform()}
    try:
        return http_json(config["gateway"] + "/v1/device/heartbeat", config["token"], "POST", payload)
    except urllib.error.HTTPError as exc:
        if exc.code != 404:
            raise
        # Legacy heartbeat both registers the device and refreshes last_seen_at.
        legacy = {"deviceId": config["device_id"], "name": "Android Termux"}
        return http_json(config["gateway"] + "/bridge/heartbeat", config["token"], "POST", legacy)

def get_tasks():
    url = "https://api.github.com/repos/" + REPO + "/issues?state=open&per_page=100"
    return http_json(url)

def load_state():
    if not STATE_FILE.exists():
        return {"completed_issues": []}
    return json.loads(STATE_FILE.read_text())

def process_pending(config):
    state = load_state()
    completed = set(int(x) for x in state.get("completed_issues", []))
    issues = get_tasks()
    candidates = []
    for issue in issues:
        if issue.get("pull_request") or issue.get("state") != "open":
            continue
        if (issue.get("user") or {}).get("login", "").lower() != OWNER.lower():
            continue
        if not (issue.get("title") or "").startswith(TASK_PREFIX):
            continue
        if int(issue["number"]) not in completed:
            candidates.append(issue)
    for issue in sorted(candidates, key=lambda item: item["number"]):
        number = int(issue["number"])
        try:
            task = json.loads(issue.get("body") or "{}")
        except json.JSONDecodeError:
            report(config, "task_failed", str(number), {"error": "Task body must be JSON."})
            completed.add(number)
            save_private(STATE_FILE, {"completed_issues": sorted(completed)[-500:]})
            continue
        action = task.get("action") if isinstance(task, dict) else None
        if action not in {"termux_diagnostics", "git_status", "git_fetch", "worker_tests", "gradle_debug_build"}:
            report(config, "task_failed", str(number), {"error": "Task action is not allowlisted."})
            completed.add(number)
            save_private(STATE_FILE, {"completed_issues": sorted(completed)[-500:]})
            continue
        report(config, "task_started", str(number), {"action": action, "issue": number})
        try:
            result = run_task(action)
            report(config, "task_finished" if result.get("ok") else "task_failed",
                   str(number), {"action": action, "result": result})
        except Exception as exc:
            report(config, "task_failed", str(number), {"action": action, "error": redact(str(exc))})
        completed.add(number)
        save_private(STATE_FILE, {"completed_issues": sorted(completed)[-500:]})
        print("Processed issue #", number, "action:", action)

def main():
    parser = argparse.ArgumentParser(description="JARVIS safe Termux task agent")
    parser.add_argument("--setup", action="store_true", help="securely configure the device token")
    parser.add_argument("--once", action="store_true", help="poll once and exit")
    parser.add_argument("--interval", type=int, default=30, help="poll interval in seconds")
    args = parser.parse_args()
    if args.setup:
        setup()
        return
    config = load_config()
    heartbeat(config)
    report(config, "agent_started", details={"agent_version": VERSION})
    print("JARVIS Termux agent connected. Polling approved GitHub task issues.")
    try:
        while True:
            try:
                heartbeat(config)
                process_pending(config)
            except urllib.error.HTTPError as exc:
                print("Network/API error:", exc.code, file=sys.stderr)
            except Exception as exc:
                print("Agent error:", redact(str(exc)), file=sys.stderr)
                try: report(config, "poll_error", details={"error": redact(str(exc))})
                except Exception: pass
            if args.once:
                break
            time.sleep(max(15, min(args.interval, 300)))
    except KeyboardInterrupt:
        pass
    finally:
        try: report(config, "agent_stopped")
        except Exception: pass
        print("JARVIS Termux agent stopped.")

if __name__ == "__main__":
    main()
