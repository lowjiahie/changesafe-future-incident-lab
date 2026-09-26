# ChangeSafe — IBM Bob Build Brief

> Planning input for IBM Bob, prepared before the Bob implementation session. This document defines the team's intended product and acceptance criteria; it is **not** evidence that IBM Bob has already built ChangeSafe. Keep the Bob task history, commits, test output, and demo evidence separate from this brief.

## Mission

**Revision: 2026-09-26.** This brief owns workflow and build scope. `CHANGESAFE_OUTPUT_CONTRACT.md` owns exact artifact formats. Follow both; ask about unresolved conflicts. The core workflow is requirement intake → project/business understanding → clarification → lightweight PRD → impact analysis → risk hypotheses → before-change tests → approval/development → replay/regression → review/goal check → evidence delivery → PR only if authorized.

Build a reusable, evidence-first developer workflow called **ChangeSafe: Future Incident Lab** inside this repository. Given a proposed code change or a Git diff, it should help a developer identify plausible downstream failures **before release**, investigate the relevant paths, create at least one executable regression check, and produce a concise go/no-go report for human review.

ChangeSafe must also be **project-native**: before generating or changing application code, Bob should learn the repository's established architecture, coding style, testing patterns, and build conventions. Existing project conventions take precedence over Bob's generic preferences, unless they conflict with an explicit user requirement or a safety constraint. Bob must identify such conflicts instead of silently imposing its own style.

This is a developer workflow powered by **IBM Bob IDE**, not an AI endpoint embedded in the Spring Boot application. The e-commerce application is the realistic system under test. Do not add an LLM API or an MCP server to the app for the MVP.

## The problem and the promised improvement

Ordinary code review often focuses on the files changed. In this event-driven e-commerce sample, one checkout change can affect Sales, Billing, Warehouse, and Shipping. A developer may overlook a downstream failure, spend time manually tracing events, or discover a missing test only after a bug occurs.

ChangeSafe should reduce that effort by turning a change into a short chain of verifiable artifacts:

`change/requirement → affected workflow → future-incident hypotheses → executable checks → test evidence → human decision`

Do not present speculative risks as confirmed bugs. Every finding must distinguish **hypothesis**, **reproduction**, **observed result**, and **remaining uncertainty**.

## Core concept carried forward from the earlier plan

The original ChangeSafe proposal called this a **Future PreMortem**: imagine a production incident caused by the proposed change *before* release. Keep that framing, but ground every imagined incident in source, requirements, a failing test, or an explicitly stated uncertainty. The distinctive engineering loop is **Incident-to-Contract**:

`plausible incident → code evidence → safety invariant → executable test → mitigation → replay → evidence pack`

A **safety invariant** is a precise property that must remain true under a relevant adverse condition. For example, if a real checkout investigation finds retry behavior that may duplicate an order, a possible invariant is “the same checkout attempt creates no more than one order.” Treat this as an example, **not** an established defect or promised feature of the current app. Bob must inspect the code before choosing the actual demo invariant.

Each finding in the report should have a simple lifecycle: `hypothesis`, `evidence-backed`, `reproduced`, `prevented`, `contained`, or `unresolved`. State transitions require evidence: a prediction alone cannot be labeled reproduced, and a passing test alone cannot claim all related risks prevented. The final **Change Evidence Pack** is the Markdown report plus the actual test output/log, relevant code references, baseline/diff, and human decision. It is not a separate server product.

The earlier plan also introduced **Guardian Skills**: optional domain-specific checks such as payment integrity, database migration, API compatibility, or accessibility. Preserve this as the extension model. A Guardian has a narrow activation trigger, least-privilege scope, short evidence-backed output, and a cost/stop condition. Route only relevant Guardians for a change; do not run every Guardian by default. For the MVP, implement the extension convention and at most **one** real Guardian only if it strengthens the chosen demo. More Guardians, a manifest/router engine, and richer incident registry are later work.

## Project facts to verify, not assume

- This repository adapts Tomas Tulka's `ttulka/ddd-example-ecommerce` under its retained MIT license. Preserve the attribution and license.
- The sample uses Java 17, Spring Boot, Maven, and an event-driven order-to-delivery flow. Read `README.md` and `pom.xml` before implementation.
- Tests use the H2 test profile; the local app can use MySQL or the H2 demo profile. Prefer synthetic data and local tests. Never commit credentials, personal information, or client data.
- The working tree may already contain unrelated/uncommitted changes. Inspect `git status` first and do not overwrite, reset, or claim them as Bob-generated work.
- Inspect the actual checkout implementation and existing tests before choosing a demo failure. A possible candidate is repeated checkout submission, **only if the current code really has an untested risk**. If it is already protected, choose a different, evidence-backed change.

## Minimum viable Bob-native structure

Ask Bob to create and validate the smallest useful set of project artifacts:

0. `.bob/rules/01-project-conventions.md` — a short, version-controlled **workspace rule** built from inspection of this repository, not from Bob's preferences. Record only supported conventions with representative file/test references, unresolved areas, and a review date/source state. Ask the team to approve it before applying it to code generation; see the first-encounter protocol below.
1. `.bob/custom_modes.yaml` — a project-level **ChangeSafe** custom mode, using supported Bob tool groups. It should analyze first, require an explicit human checkpoint before app-code edits, allow focused `explore` subagents when justified, and keep outputs evidence-based. Do not override Bob's built-in modes.
2. `.bob/skills/changesafe/SKILL.md` — the reusable workflow procedure. Keep it concise; place long checklists or templates in supporting files. The skill must accept a specific change, branch diff, or requirement and define the ordered phases below.
3. `.bob/commands/changesafe.md` — a short user-facing entry point, if it works in the installed Bob version. Avoid duplicating the full skill instructions in the command.
4. `changesafe/templates/risk-report.md` — a human-readable report template with: change summary; evidence-linked affected components; ranked future-incident hypotheses; test plan; actual commands/results; unresolved risk; reviewer decision. Include a compact **before/after and goal-check** section. Provide file paths and observations, not invented certainty.
5. `changesafe/README.md` — how another developer runs the workflow, adds a specialized skill/checklist later, and understands the boundary between Bob IDE and the Java app.
6. `changesafe/scripts/run-targeted-tests.ps1` (or an equally small Windows-friendly script) — a deterministic test gate for this Java/Maven project. It should run explicitly selected existing tests with the H2 test profile, return a meaningful exit code, save a **new, non-overwriting** full local log for each run, and print a **short** PASS/FAIL/UNKNOWN summary containing the command, selected tests, result, elapsed time, and log path. It must not send test output to any external service, hide failures, or treat a skipped test as PASS.
7. An optional project-level `.bob/agents/change-impact.md` persona **only if the installed Bob version supports project agent personas**. Validate its format in Bob before creating it. Do not make this file a prerequisite for the core workflow; a focused built-in `explore` subagent is sufficient for the MVP.
8. `.bobignore` — a short, reviewed exclusion list for generated/build directories and any actual local secrets discovered in the repo. Do not ignore relevant source, tests, ChangeSafe templates, or evidence needed for the demo. Direct `@file` mentions can bypass ignore settings, so never explicitly mention a secret file to Bob.
9. `changesafe/templates/change-brief.md` and `changesafe/templates/business-rules.md` — exact templates from the Output Contract. Materialize `changesafe/business-rules.md` as an initially empty decision register: never invent confirmed business rules. Each runtime investigation creates its own `change-brief.md` in its evidence directory.
10. `changesafe/templates/comparison.md`, `changesafe/templates/pr-description.md`, and `changesafe/scripts/validate-output.ps1` — materialize the fixed comparison and PR formats, plus a small deterministic validator for required headings, statuses, AC consistency, relative links, and available evidence. A validator cannot establish business correctness or verify screenshot authenticity; human review remains necessary.

Do **not** build a dashboard, server integration, autonomous deployment gate, risk-scoring service, or broad test suite in the first iteration. Those are optional extensions, not MVP requirements.

## First-encounter project-convention protocol

Before Bob creates ChangeSafe code, tests, scripts, or application fixes, it should perform a **bounded repository onboarding pass**:

1. Read existing guidance first: `README.md`, `pom.xml`, applicable `AGENTS.md`/Bob rules if present, test configuration, and only representative source/test files from the affected packages. Inspect `git status` so uncommitted user changes are not mistaken for settled conventions.
2. Extract **observed** conventions: Java version and build commands; package/domain boundaries and event usage; naming/immutability/validation patterns; controller/service/data-access patterns; testing framework, profile, and fixture style; and frontend/template conventions only if that area is relevant. Attach paths to each claim. Do not canonize a one-off pattern or a known bug.
3. Write a concise draft `.bob/rules/01-project-conventions.md` following the fixed format in `CHANGESAFE_OUTPUT_CONTRACT.md`. Mark contradictions or missing evidence as `UNCONFIRMED` and ask the team to resolve them. Do not invent a rule to fill a gap.
4. Request human review of the draft rule. Once approved, use it for subsequent ChangeSafe-generated code and tests. If a requested change needs a different pattern, explain the conflict and obtain approval rather than silently rewriting project style.
5. On later runs, reuse the rule to avoid repeated repository-wide exploration, but re-check the relevant convention against touched files. Update the rule only when the project's actual conventions change, with a source reference and reviewer approval.

For a **different repository**, run this first-encounter protocol again. Never copy this e-commerce project's Java-specific rule into another project's codebase as if it were universal.

The rule should be short because Bob loads workspace rules into future tasks. It is a guardrail, not a full architecture essay. Existing project behavior is evidence, but not automatic authority: insecure or broken code must not be memorialized as a standard. For any generated application-code diff, report which project conventions were followed and call out deviations explicitly.

## Additional IBM Bob features to use deliberately

These are small additions to the workflow, not new services or a larger agent swarm:

| Bob feature | ChangeSafe use | Cost/safety boundary |
| --- | --- | --- |
| Context mentions: `@git-changes`, targeted `@/file`, `@terminal`, `@problems` | Give Bob the exact change, relevant source, test failure, or IDE diagnostics instead of a pasted full-repo dump. | Mention only the smallest relevant material; `@file` can bypass `.bobignore`. |
| Document understanding | If the user supplies a ticket/PDF/DOCX/spec, extract exact acceptance criteria and unresolved questions before the risk analysis; cite the source in the report. | Optional input, not a requirement to attach a large document on every run. |
| Task-level approvals | Allow read-only discovery and safe focused tests; require review/approval for app-code edits and commands that modify data or use external services. | Never enable broad auto-approval merely to speed up the demo. |
| Built-in `/review` | After the fix, review the actual diff (including uncommitted changes when appropriate) and carry relevant findings into the evidence report. | Human review remains required; do not claim `/review` proves safety. |
| Git checkpoints and Bob Rollback | Establish an identifiable before-fix state and recover from an unwanted Bob change. | The current working tree may already be dirty. Preserve user edits; do not use rollback or reset until its exact effect on those edits is understood and approved. |

**Later only, if the MVP is already complete:** lifecycle hooks could run a deterministic output-contract checker or log task events, but they add configuration and failure modes. Do not implement hooks just to claim another Bob feature. Bobalytics is not an MVP dependency; the team's actual Bobcoin gauge and mandatory `bob_sessions/` screenshots are the budget evidence.

## Agent structure and responsibilities

ChangeSafe is **one main Bob workflow with conditional delegation**, not a permanently running swarm. Keep ownership of the final report and the human approval checkpoint in the main task.

| Role | Bob mechanism | Runs when | Responsibility and output |
| --- | --- | --- | --- |
| ChangeSafe coordinator | Project custom mode + ChangeSafe skill in the main Bob task | Every ChangeSafe run | Define scope, run the cheap test gate, decide whether deeper analysis is warranted, combine evidence, request approval before app-code edits, write the final report. |
| Impact explorer | Read-only `explore` subagent; optional matching project persona | Only for a real cross-module question | Trace the changed path through Sales, Billing, Warehouse, or Shipping. Return relevant file paths, event sequence, one concrete failure possibility, and unknowns. Do not edit. |
| Test-gap explorer | A second read-only `explore` subagent, initially prompted without a dedicated persona | Only if test coverage is unclear **and** this investigation is independent of the impact exploration | Find existing relevant tests and a missing behavior check. Return test paths, what is and is not covered, and one proposed test. Do not edit. |
| Verifier/fixer | Main Bob task in an edit-capable mode after human approval | Only when a high-value risk needs validation or a confirmed bug needs fixing | Add a focused test, run it, make a minimal authorized fix, rerun tests, and report actual results. This is **not** an always-on extra subagent. |

Each delegated task gets a small context packet: exact change/diff, bounded question, relevant paths, and required output format. It should return a short evidence summary, not its entire investigation transcript. The coordinator must verify important claims before using them in the report. Never spawn both explorers automatically; zero or one is normal, two is the maximum for the MVP.

For future extension, allow a contributor to add a specialized project agent persona or skill (for example, `payment-risk`, `database-migration`, or `security`) with a clear trigger, read/edit permissions, evidence format, and cost/stop conditions. The coordinator should select it **only** when the change matches that domain. The initial demo should not implement all of these hypothetical agents.

## Required ChangeSafe workflow

A short prompt such as `/changesafe Improve checkout` must invoke this procedure. Users must not have to repeat clarification, impact, testing, or evidence instructions.

1. **Receive requirement:** establish the goal, scope, and comparison source state. Ask about priorities if the request is too broad.
2. **Understand project:** reuse approved project rules; first-time onboarding drafts observed conventions for review. Preserve existing user edits.
3. **Understand business and clarify:** read supplied documents and confirmed decisions. Distinguish intended behavior from current implementation. Proactively ask the requester, in simple language, about material missing/conflicting rules even if the prompt does not request questions. Reuse confirmed rules; record new decisions in `changesafe/business-rules.md`. Never silently decide stock reservation timing, backorders, partial checkout, delivery requirements, or submission identity. Pause dependent implementation while answers are pending; safe independent analysis may continue.
4. **Plan + lightweight PRD:** create the run's `change-brief.md` using the fixed template, with measurable AC IDs, scope, exclusions, unresolved questions, and a compact plan. Small changes require a short brief, not a long PRD.
5. **Impact analysis:** trace relevant calls, events, persistence, and downstream components, including unedited code. Cite actual evidence and select relevant regression checks.
6. **Predict failures:** identify at most three concrete risk hypotheses and select a precise safety invariant. Use zero to two read-only explorers only for useful independent questions. Hypotheses are not confirmed defects.
7. **Generate/reuse tests + before-change baseline:** reuse existing valid-behavior tests, add acceptance and adverse-condition checks with authorized test-file edits, and execute before changing application behavior. Classify failures as pre-existing failures, unmet new requirements, reproduced defects, or environment issues. Preserve immutable execution logs and summaries. A cheap existing-test gate may run earlier once scope is clear; it does not replace this comparable baseline.
8. **Approval + develop:** present findings, proposed changes, and scope for human approval before app-code edits. Implement a focused authorized solution following project conventions. Separate intentional purchases by one user from replay of one submission intent.
9. **Replay + complete coverage:** rerun the same checks/settings and relevant existing tests; add implementation-specific tests where necessary. Return to development for technical failures, or clarification for newly material business uncertainty. Never weaken expectations to get green tests. Stop and report blockers instead of looping indefinitely.
10. **Review + goal check:** review the actual diff with Bob review when available. Evaluate ACs as MET, NOT MET, or NOT VERIFIED using evidence. Test success does not establish complete requirement coverage, automatic release approval, or absence of production incidents.
11. **Deliver evidence:** generate fixed-format reports, log links, source states, unresolved risks, and a presentation-ready comparison. Remind the team to capture real Bob task consumption summaries manually. Incomplete runs still produce honest reports; never fake missing executions.
12. **Optional PR:** only with explicit authorization, commit/push/create a PR using the fixed PR-description template and configured Git tooling. Otherwise hand off locally. Human review owns merge and release decisions.

Business clarification is a cross-cutting checkpoint: it can occur during analysis, development, or testing, not only after replay. New answers update the change brief and test expectations with traceable approval; preserve previous run evidence.

### Controlled incident-lab evaluation

The team may deliberately seed flaws in a separately identified lab baseline using synthetic fixtures. Label these as controlled defects, not naturally discovered production incidents. Do not introduce a defect during evaluation merely to force a red-to-green story.

Candidate scenarios: two authenticated synthetic users compete for one stock unit; the same submission intent is replayed and must not duplicate orders/stock deductions; physical delivery checkout lacks a required address. All expected behavior must be confirmed before implementation. Complete one scenario first.

During discovery evaluation, supply business requirements but not the defect locations or solutions. Keep the team's answer key outside Bob-accessible task context and record every hint. Claim independent discovery only if the actual session supports it.

## Demo and acceptance criteria

Produce one end-to-end demonstration on a **real change** in this repository. It is successful when a reviewer can see:

- The initial requirement or code diff and a baseline commit/hash.
- Bob's impact map with at least two relevant parts of the application, when the chosen change genuinely crosses modules.
- A future-incident hypothesis grounded in code, not generic advice.
- The deterministic test gate's selected tests, short summary, exit status, and retained full log.
- A new focused test or deterministic reproduction for the selected risk plus its actual result.
- A before/after comparison if a confirmed bug is fixed; otherwise an honest risk assessment and test coverage improvement.
- One selected incident linked to a written safety invariant and its executable check; mark its lifecycle status with supporting evidence.
- A saved, presentation-ready comparison that answers **“Did we achieve the stated goal?”** criterion by criterion, without needing to rerun the app or tests live.
- The ChangeSafe mode/skill can be run again on another proposed change without rewriting the procedure.
- Generated code and tests follow the approved project rule; the report cites applied conventions and explicitly documents any approved deviation.
- A short comparison of manual effort versus ChangeSafe effort, measured or clearly labeled as an estimate. Never invent productivity percentages.

## Presentation-ready before/after evidence

For the chosen demo, Bob must create `changesafe/evidence/<demo-name>/comparison.md` **after running the work**, not fill it with planned or illustrative outcomes. The file should be readable as a standalone presentation artifact and include:

| Field | Required content |
| --- | --- |
| Goal | The exact requirement, acceptance criteria, and selected safety invariant agreed before the fix. |
| Comparable states | Clearly identified **before-fix** and **after-fix** source states (commit hashes when available; otherwise labeled working-tree states plus the relevant diff/status). Do not mislabel an uncommitted state as a commit. |
| Execution | The same focused test and settings in both states where feasible: command, Java version/test profile, timestamp, exit code, test count, and elapsed time. Explain any necessary difference, such as a new regression test being added before the fix. |
| Raw evidence | Separate, preserved pre-fix and post-fix logs, plus concise summaries and links/paths to the relevant test and changed code. Never overwrite or fabricate a result. |
| Goal check | One row per acceptance criterion: `MET`, `NOT MET`, or `NOT VERIFIED`, with a concrete observation. Report remaining risks and whether the incident is reproduced, prevented, contained, or unresolved. |
| Impact | Measured developer effort and Bobcoins if recorded; otherwise label estimates as estimates and omit unsupported percentage claims. |

The comparison should end with a short **demo takeaway** explaining what changed and what the evidence proves. If no bug is confirmed, compare coverage/understanding before and after honestly; do not force a red-to-green story. The document, logs, and any manually captured screenshots should be enough to present the result **without running the system again on stage**. A live rerun may be offered as a backup, not as the only proof.

## Required output formats and page layout

**Normative specification:** [CHANGESAFE_OUTPUT_CONTRACT.md](CHANGESAFE_OUTPUT_CONTRACT.md) fixes the exact file names, heading text, column order, JSON keys, status values, and screenshot conventions. Bob must copy its templates verbatim and validate completed reports against it. The short outlines below are illustrative only; where they differ, the Output Contract takes precedence. Do not improvise a new layout for each run.

Use simple, version-control-friendly files. The Bob implementation should create the actual templates from the Output Contract; this brief is not a completed report. Each **ChangeSafe run** gets its own evidence directory, while hackathon **Bob task consumption screenshots** go in the repository-root `bob_sessions/` directory.

```text
<repo>/
├── bob_sessions/
│   ├── 01-plan-consumption.png
│   ├── 02-build-consumption.png
│   ├── 03-demo-consumption.png
│   └── INDEX.md                       # optional human-written index; never substitutes for PNGs
└── changesafe/evidence/<demo-name>/
    ├── risk-report.md                 # full engineering report for this run
    ├── comparison.md                  # concise, presentation-ready before/after
    ├── logs/
    │   ├── before-test.log            # original command output, not paraphrased
    │   └── after-test.log
    └── screenshots/                   # optional additional demo evidence
```

The exact number of `bob_sessions/*.png` files must match the Bob task/session consumption summaries actually used for the hackathon; the three names above are **examples, not a fixed count**. Number files chronologically and include a short task purpose in the filename. Do not create placeholder images.

### Fixed formats, not illustrative alternatives

Copy the exact templates from `CHANGESAFE_OUTPUT_CONTRACT.md`. Do not keep a second competing layout in this brief. Every run uses the contract's `change-brief.md`, `risk-report.md`, and `comparison.md`; execution JSON/logs exist only for real executions. The shared business decision register is not an automatically loaded repository-wide context dump.

## Budget and safety

The two-person team has **40 Bobcoins per member (80 combined, not assumed transferable)**. Build one scenario first. Use focused context, short outputs, and at most two investigation subagents. Check the Bobcoin gauge between phases and preserve budget for testing and review. Leave MCP disabled unless a concrete need emerges.

Keep human approval for file edits and commands that change data. Prefer H2 test data. Never run destructive database commands against a database containing real data. Preserve the existing working tree and the upstream attribution.

## Token-efficient design requirements

ChangeSafe must be **selective by default**, not a full-repository multi-agent scan on every run. Bobcoins and raw tokens are related but are not interchangeable; do not promise a fixed cost per run. Record actual Bobcoin usage during the demo.

1. **Cheap intake first:** identify the baseline and changed files with Git (`git diff --name-only`, relevant diff hunks) and use local search/tests to narrow the candidate paths. These deterministic steps need no additional AI service. Avoid broad folder `@mentions`, huge logs, and entire-document attachment when a relevant section is enough.
2. **Test script before deep reasoning:** run selected existing tests as a deterministic filter. Keep the complete raw output in a local log for audit, but expose only the concise summary to Bob by default. Read failure details from the log only when needed. Running a script through Bob still has some Bobcoin cost; the intended saving is reduced context and unnecessary reasoning, not zero-cost execution.
3. **Two-pass analysis:** first produce a small change summary and examine the test-gate result. If it is a local, low-risk edit with adequate passing coverage, stop with a short report. A failing/unknown result, missing coverage, or meaningful downstream risk proceeds to future-incident investigation. A stop is a legitimate ChangeSafe result, not a failure.
4. **Minimal context packets:** each investigation receives only the change summary, the specific question, and the relevant file paths/diff. Do not pass the full conversation history to subagents unless essential. Ask each subagent for a bounded answer: evidence, risk, proposed check, uncertainty.
5. **Conditional parallelism:** use no subagent for a simple change; use one `explore` subagent for a focused cross-module question; use two only when the questions are truly independent. Do not spawn `general` subagents just to read code. Parallelism may save elapsed time but does not guarantee lower Bobcoin consumption.
6. **Bound outputs and tests:** list at most three incident hypotheses, select the highest-value one for an executable check, and run targeted Maven tests before the full suite. Keep terminal output concise while preserving the actual command, exit status, and relevant failure lines.
7. **Reuse without stale conclusions:** keep the workflow procedure and report template in versioned files. Reuse prior evidence only when the baseline commit and relevant code have not changed; otherwise re-check it. Never copy an old PASS result into a new report as if freshly run.
8. **Meter and stop:** check remaining Bobcoins at the intake, post-investigation, and pre-fix checkpoints. If the budget is low, stop after an honest report and leave proposed tests/fixes for a later run. Do not spend the final balance polishing prose or generating an optional dashboard.

The resulting architecture is a **small controller workflow plus opt-in specialist skills/checklists**. New specialists can be added later, but their instructions should load only when relevant to the changed area. Do not force every future user to run every specialist.

## Explicitly deferred from the original concept

The previous long-form plan described order cancellation and refunds, three Guardians, automated release approval, a Guardian Router, incident registry, dashboard, fault-injection platform, CI/MCP integrations, and sample metrics such as “120 minutes to 20 minutes.” These are **concept examples or future roadmap items**, not claims about the current codebase and not required for this 40-Bobcoin MVP. Do not silently build a cancellation/refund feature or present illustrative numbers as measured results. Use the existing application and one verified scenario; keep the release recommendation advisory and the final decision human-owned.

## Evidence capture — human-owned, not assumed automatic

Hackathon submission requirement supplied by the team: **“Upload all task session consumption summary screenshots to the `bob_sessions` folder in your code repository.”** Treat this as mandatory. Bob keeps IDE task history, but this brief does not assume that it automatically saves submission-ready consumption-summary screenshots or session export files. Do not label a hand-written summary as an exported Bob session.

For **every Bob task/session used**, open its actual consumption summary in Bob, capture the screen manually, and save the image under the root `bob_sessions/` directory. Check that the summary and task identity are readable. Keep a simple index mapping task/session to screenshot, purpose, and corresponding artifact; the index is organizational only. A new Bob task means a new required screenshot. Do not wait until the final presentation in case the task history is cleared or inaccessible.

Separately, take optional **demo milestone** screenshots of: (1) Bob's plan/impact map, (2) focused subagent evidence if used, (3) the failing/baseline test, (4) the passing test or honest unresolved finding, and (5) the final before/after report/review. Save those under `changesafe/evidence/<demo-name>/screenshots/`; they do **not** replace the mandatory `bob_sessions/` consumption screenshots. Keep all screenshots free of credentials and personal data. Do not create placeholder screenshots or fabricate logs. If Bob exposes a session export in the installed version, use the product UI and preserve the original file unchanged as extra evidence.

## Bootstrap: structure first, demo later

### Stage A — Plan mode, no file edits

```text
Read @/CHANGESAFE_BOB_BUILD_BRIEF.md and @/CHANGESAFE_OUTPUT_CONTRACT.md as the build specification. Inspect README, pom.xml, git status, and representative source/tests. Do not edit files yet. Identify observed project conventions and propose the smallest ChangeSafe structure, supported Bob configuration, exact templates, scripts, and validation plan. Flag contradictions or unsupported features. Wait for my approval.
```

### Stage B — Agent mode, after approving the plan

```text
Implement only the approved ChangeSafe scaffold from the Build Brief and Output Contract. Draft the project rule for my approval before generating code/tests/scripts. After that approval, create the supported mode, skill, /changesafe command, templates, empty business-decision register, README, targeted-test script, and output validator. Make proactive business clarification the default. Validate the scaffold and explain how to invoke it. Do not change application behavior, seed defects, run a repair demo, invent evidence, commit, push, or create a PR. Stop after structure validation.
```

If a configuration feature is unsupported, document a supported fallback rather than silently creating an inert file. Verify mode and command discovery in the installed IDE; file existence alone is insufficient. Test script/validator checks should use synthetic temporary fixtures where appropriate, clearly separated from demo evidence. Validate fail/pass/unknown/no-tests behavior and non-overwriting evidence handling. Report checks not executable in the environment.

### Stage C — New task, using the configured ChangeSafe entry point

```text
/changesafe
Review the checkout flow for potential problems. Explain findings in simple language and do not change application code until I approve.
```

Do not give the hidden defect answer key to the discovery task. Provide or confirm necessary business requirements when asked. Keep the build and evaluation tasks distinct and capture consumption screenshots for every task from both members.

## Final prompt to use in IBM Bob (after the demo work)

```text
Create the final ChangeSafe evidence pack for this demo. Use the actual pre-fix and post-fix test runs, source states, relevant diff, and recorded Bobcoin/time measurements. Save a standalone before/after comparison to changesafe/evidence/<demo-name>/comparison.md, with separate links to both raw logs. For every agreed acceptance criterion, mark MET, NOT MET, or NOT VERIFIED and cite the observation. Explain whether the selected future incident was reproduced and prevented, contained, or left unresolved. Do not invent measurements or claim a clean PASS for tests that were skipped. Make the report usable in a presentation without rerunning tests live; if required evidence is missing, identify it explicitly before finalizing.
```

## Primary IBM Bob documentation

- [Modes and best practices](https://bob.ibm.com/docs/ide/getting-started/best-practices)
- [Custom modes](https://bob.ibm.com/docs/ide/configuration/custom-modes)
- [Skills](https://bob.ibm.com/docs/ide/features/skills)
- [Slash commands](https://bob.ibm.com/docs/ide/features/slash-commands)
- [Subagents](https://bob.ibm.com/docs/ide/features/subagents)
- [Context mentions](https://bob.ibm.com/docs/ide/features/context-mentions)
- [Code reviews](https://bob.ibm.com/docs/ide/features/code-reviews)
- [Custom rules](https://bob.ibm.com/docs/ide/configuration/rules)
- [Lifecycle hooks — optional later](https://bob.ibm.com/docs/ide/configuration/lifecycle-hooks)
- [Bobcoins](https://bob.ibm.com/docs/ide/account/bobcoins)
