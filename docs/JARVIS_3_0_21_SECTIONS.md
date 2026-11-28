# JARVIS 3.0 — 21-section system map

This document defines the 21 user-facing sections as one product architecture. The Android app exposes the catalog in a dedicated **21 Sections** tab. Opening a section switches to the shared conversation and adds that section's focus to the request context; it does not create 21 separate agents or imply every integration is complete.

## Shared system contract

Every section uses the same central conversation, provider gateway, opt-in profile memory, conversation history, and tool boundary. Sections must not create shadow stores of secrets or bypass Android permission prompts. Actions that send, publish, delete, purchase, modify security settings, or disclose personal data require fresh explicit confirmation immediately before execution.

Each capability exposes its real implementation state. FOUNDATION means a related foundation already exists in the current app, not that the complete section is finished. PLANNED means the section is part of the product map but its specialist workflow/integrations are not yet implemented.

## The 21 sections

| # | Section | Responsibility | Current state |
|---:|---|---|---|
| 1 | Command Centre | Status, overview, quick actions and activity | Foundation |
| 2 | AI Brain | Intent classification, reasoning and provider orchestration | Foundation |
| 3 | Voice Assistant | Speech input, spoken replies and future wake-word controls | Foundation |
| 4 | Personal Memory | Searchable conversation history and opt-in profile | Foundation |
| 5 | Autonomous Agent | Multi-step planning, execution tracking and safe recovery | Planned |
| 6 | Phone Control | Permission-gated Android actions and per-action approval | Foundation |
| 7 | Communication | Assisted messaging, email and social workflows | Planned |
| 8 | Research Lab | Web/scientific research, citations and evidence quality | Foundation |
| 9 | Knowledge Centre | General and specialist knowledge workflows | Planned |
| 10 | Finance & Markets | Company/market analysis and sourced live financial data | Planned |
| 11 | Health & Medicine | Evidence-based health information with clinical boundaries | Planned |
| 12 | Fitness & Nutrition | Training, recovery and nutrition planning | Planned |
| 13 | Food Science | Food safety, quality, processing and formulation | Planned |
| 14 | Education | Tutoring, study plans and explanations | Planned |
| 15 | Scholarship Manager | Eligibility filtering, deadlines and application materials | Planned |
| 16 | Files & Gallery | User-selected files, gallery indexing and document workflows | Foundation |
| 17 | Productivity | Tasks, reminders, schedules and personal workflows | Planned |
| 18 | Security Centre | Privacy, permission visibility and authorized security guidance | Planned |
| 19 | Developer Studio | Code, repository inspection, debugging and tests | Planned |
| 20 | Internet & Tools | Approved external tools, calculations and connected services | Planned |
| 21 | Self-Improvement | User-approved evaluations, feedback and transparent updates | Planned |

## Implementation sequence

1. Section shell: catalog, 21-section navigation, honest state labels, and shared-conversation handoff.
2. Reliable orchestration: structured task classification, specialist context templates, tool schemas, provider metadata, retries and observable failure states.
3. Useful autonomous workflows: task plans with checkpoints, resumable state, bounded retries, and a visible action log. Read-only actions can run within granted scopes; consequential actions pause for approval.
4. Specialist packs: research, food science, scholarships, developer tools, education, finance, health and fitness. Each pack needs domain tests and source/freshness rules.
5. Android integration: reminders/background work, app-specific integrations and stronger accessibility workflows, always within Android platform permissions and user consent.
6. Evaluation and release: unit tests, emulator tests, gateway contract tests, security/privacy tests, then physical-device validation on the owner's Redmi Note 13 4G.

## Acceptance criteria

- All 21 sections are visible and individually openable.
- Opening a section preserves the shared conversation and passes a clear specialist focus into its context.
- The UI distinguishes existing foundations from planned capabilities.
- No section claims unsupported access, background autonomy, offline intelligence, or completed specialist integrations.
- Sensitive actions continue to require explicit, action-specific approval.
- CI must build the Android app and run tests before an APK is considered releasable.
- A successful CI run does not replace a real-device test or prove live Worker/provider configuration.
