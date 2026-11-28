# JARVIS Termux companion agent

This agent is an outbound-only companion for a user's own Termux checkout. It does not expose SSH, listen on a network port, or accept arbitrary shell commands.

## Supported tasks

The agent polls this public repository for issues created by `Manekdm13` whose title starts with `[JARVIS TASK]`. Task titles and JSON bodies are public because this repository is public; never put secrets, personal information, or private command output in a task. The issue body must be a JSON object containing one fixed `action` value:

- `termux_diagnostics`
- `git_status`
- `git_fetch`
- `worker_tests`
- `gradle_debug_build`

No shell command, arguments, script text, release, push, delete, credential, or phone-control action is accepted from issue content. The worker records heartbeats and task results in the isolated `jarvis-device-bridge` D1 database.

## Install on Termux

Install Python and Node.js (for Worker tests):

```sh
pkg install python nodejs-lts -y
```

From the repository root:

```sh
python scripts/jarvis_termux_agent.py --setup
```

Paste the device bridge token only into the hidden prompt. It is stored in `~/.config/jarvis-termux-agent/config.json` with restrictive file permissions.

Start a one-time connection test:

```sh
python scripts/jarvis_termux_agent.py --once
```

Then start continuous polling:

```sh
termux-wake-lock
python scripts/jarvis_termux_agent.py
```

Keep Termux running and exempt it from Android battery optimization for reliable background polling. Android may still stop background processes under memory pressure.

## Task format

A task issue created by the repository owner uses a title such as `[JARVIS TASK] Check Termux toolchain` and a JSON body such as:

```json
{"action":"termux_diagnostics"}
```

The agent reports status to the authenticated Worker endpoint. Results are visible to the connected Cloudflare account through D1 audit records. ChatGPT can submit task issues through the connected GitHub integration and inspect the resulting D1 audit events; this is an outbound polling bridge, not a raw SSH session.

## Security

- The agent only makes outbound HTTPS requests.
- The bridge token is entered through a hidden prompt and is never printed.
- Task issuer is checked against the repository owner.
- Actions are a fixed allowlist; no arbitrary shell is evaluated.
- Build/test output is truncated and basic credential patterns are redacted.
- Keep the token private. Revoke/rotate it if the phone is lost.
