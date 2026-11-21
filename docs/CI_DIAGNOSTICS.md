# JARVIS CI Diagnostics

## Purpose

Keep Android emulator failures distinguishable from Worker checks and APK compilation failures. A successful Worker test or APK build does not prove that the Android emulator smoke test passed.

## Current verification boundary

- Pull request: #12 (`fix/jarvis-chat-gateway-ci`).
- Worker regression tests, TypeScript check, Wrangler dry-run, and debug APK build have passed in the previously inspected run.
- The Android emulator smoke-test has failed in an earlier run, but the available job log download returned `BlobNotFound`. The exact failure stage was therefore not established.
- The latest observed workflow run was queued/pending when inspected. No jobs or artifacts were available from that run yet.
- Do not describe Gemini chat as verified until a real authenticated request succeeds from the installed Android app through the Worker to Gemini.

## Required evidence for an emulator failure

The workflow should upload `jarvis-emulator-diagnostics` even when the emulator action exits before running its test script. The artifact should include, where available:

- `adb devices -l`
- `adb shell getprop`
- `adb logcat -d -v threadtime`
- `adb shell dumpsys activity activities`
- Gradle connected-test output and Android instrumentation XML/HTML reports
- Runner disk-space details

## Triage sequence

1. Fetch the latest workflow run and its jobs; identify the exact failed job and step.
2. Fetch the emulator diagnostics artifact before changing emulator settings.
3. Separate emulator boot verification from `gradle connectedDebugAndroidTest` so startup failures and test failures are independently visible.
4. If boot fails, inspect emulator action output, KVM access, disk space, adb state, and system logs.
5. If boot succeeds but instrumentation fails, inspect logcat, test reports, app launch/crash traces, and the failing assertion.
6. Change only the component implicated by evidence, then rerun the failed job.
7. Record the run ID, failing step, root cause, code change, and successful verification here.

## Acceptance criteria

- Worker tests, TypeScript check, and Wrangler dry-run pass.
- Android debug APK builds and is uploaded.
- Emulator boots and reports `sys.boot_completed=1`.
- `connectedDebugAndroidTest` passes with instrumentation reports uploaded.
- Separately, an authenticated app-to-Worker-to-Gemini chat is tested on a physical device before claiming online AI integration is complete.
