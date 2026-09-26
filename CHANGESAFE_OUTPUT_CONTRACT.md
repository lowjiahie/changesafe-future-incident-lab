# ChangeSafe Output Contract v2

> This is a **planning specification**, not a completed IBM Bob output. When building ChangeSafe, IBM Bob must create the reusable templates and make the workflow fill them **without changing their headings, column order, file naming, or status vocabulary**. Only the values inside the templates vary by run.

## 1. Universal formatting rules

- Language: English, so the hackathon judges can read every artifact. Encoding: UTF-8. Use GitHub-flavored Markdown for `.md`, JSON for `.json`, plain text for `.log`, and PNG for screenshots.
- One ChangeSafe run = one directory: `changesafe/evidence/<run-id>/`, where `<run-id>` is lowercase kebab-case such as `checkout-retry-01`. Never overwrite a prior run.
- Every run also includes `change-brief.md` in the fixed format below. Shared confirmed business decisions live in `changesafe/business-rules.md`; do not regenerate or overwrite past decisions per run. PR descriptions are optional and never imply authorization to create a PR.
- Required run files: `risk-report.md`, `comparison.md`, `before-test-summary.json`, `after-test-summary.json`, `logs/before-test.log`, and `logs/after-test.log` **when both executions occurred**. If a run stopped early, keep the same Markdown sections and mark the unavailable execution `NOT RUN — <reason>`; do not create fake logs or JSON.
- Dates/times use ISO 8601 with an offset, for example `2026-09-25T16:17:49+08:00`. Durations are measured seconds. Numeric metrics must come from actual commands or recorded measurements.
- All fixed headings below must appear in the stated order. Do not rename headings, reorder columns, add decorative sections, or replace tables with free-form prose. Use `N/A — <reason>` for a field that does not apply and `NOT VERIFIED — <reason>` for missing evidence. Never leave a required field blank except the explicit human-decision fields.
- Status vocabulary: test `PASS | FAIL | UNKNOWN | NOT RUN`; criterion `MET | NOT MET | NOT VERIFIED`; incident `HYPOTHESIS | EVIDENCE-BACKED | REPRODUCED | PREVENTED | CONTAINED | UNRESOLVED`; overall goal `MET | NOT MET | NOT VERIFIED`. A claim of `PREVENTED` requires a reproduced pre-fix failure and a passing post-fix replay of the same check. `MET` requires evidence for the corresponding criterion.
- Risk IDs use `R-01`, `R-02`, `R-03` in report order. Use relative Markdown links to actual repository files/logs. Do not invent file paths, line numbers, tests, Bobcoin amounts, or screenshots.
- The final human go/no-go decision is **never auto-filled by Bob**. The main ChangeSafe report may offer an advisory recommendation, clearly labeled as such.

## 2. Exact `risk-report.md` template

IBM Bob should materialize this as `changesafe/templates/risk-report.md`, then copy and fill it for each run. Keep the heading text and table columns exactly as shown.

```markdown
# ChangeSafe Risk Report — <change-title>

| Field | Value |
| --- | --- |
| Run ID | <run-id> |
| Generated at | <ISO-8601 timestamp> |
| Requirement / change | <one-sentence description and source link> |
| Before-fix source state | <commit hash, or explicitly labeled working-tree state> |
| After-fix source state | <commit hash, or explicitly labeled working-tree state; N/A if no fix> |
| Bob task ID / title | <actual task reference; NOT VERIFIED if unavailable> |
| Project rule | <link to reviewed .bob/rules/01-project-conventions.md or NOT VERIFIED — awaiting review> |
| Change brief | [change-brief.md](change-brief.md) |
| Business decisions | <specific BR IDs and link to decision register, or NOT VERIFIED — awaiting clarification> |
| Report status | COMPLETE / NEEDS REVIEW / INCOMPLETE |

## 1. Goal and scope

**Goal:** <one measurable sentence>

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | <criterion> | <test or observation> |

**In scope:** <files, modules, and diff/reference>  
**Out of scope:** <explicit exclusions or N/A — none>

**Unresolved business questions:** <question IDs and implementation blocked, or N/A — none>

## 2. Cheap test gate

| Selected tests | Command | Result | Exit code | Duration (s) | Summary / raw log |
| --- | --- | --- | --- | --- | --- |
| <test names> | <exact command> | PASS / FAIL / UNKNOWN / NOT RUN | <number or N/A> | <measured or N/A> | <links or N/A> |

**Reason to stop or continue:** <one evidence-based sentence>

## 3. Change-impact map

| Component | Direct / downstream | Observed path or dependency | Evidence |
| --- | --- | --- | --- |
| <module> | <type> | <actual relationship> | <code/document link> |

## 4. Future incidents

| Risk ID | Trigger and incident | User / system impact | Evidence | Confidence | Status |
| --- | --- | --- | --- | --- | --- |
| R-01 | <specific failure chain> | <impact> | <links or NOT VERIFIED> | HIGH / MEDIUM / LOW | HYPOTHESIS / EVIDENCE-BACKED / REPRODUCED / PREVENTED / CONTAINED / UNRESOLVED |

**Selection rationale:** <why the highest-value risk was selected; N/A if none>

## 5. Selected safety contract

| Field | Value |
| --- | --- |
| Linked risk | <R-01 or N/A> |
| Safety invariant | <one precise, testable condition> |
| Adverse condition | <retry, invalid input, timeout, etc.> |
| Reproduction / regression test | <test file and test name link> |
| Expected result | <observable expectation> |
| Actual before-fix result | <observation and log link, or NOT RUN — reason> |
| Actual after-fix result | <observation and log link, or NOT RUN — reason> |

## 6. Implementation and verification

| Item | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test | <status / observation> | <status / observation> | <before and after links> |
| Relevant existing tests | <count / status> | <count / status> | <links> |
| Application behavior | <observed> | <observed> | <code/test links> |

**Approved code changes:** <short list and links, or N/A — no change>  
**Implementation approval:** <requester, scope, timestamp, and actual task reference; or PENDING — no app-code edits>  
**Project conventions followed:** <rule items and representative code/test evidence, or N/A — no generated code>  
**Approved deviations:** <reason and reviewer approval, or N/A — none>  
**Review findings:** <actual findings or N/A — not run>

## 7. Goal check and remaining risk

| ID | Criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | <same text as section 1> | MET / NOT MET / NOT VERIFIED | <actual result and link> |

**Overall goal:** MET / NOT MET / NOT VERIFIED — <one-sentence rule-based explanation>  
**Remaining risk:** <what is still unknown or N/A — none identified within scope>  
**Bob advisory recommendation:** <proceed / investigate / do not proceed, with reason; advisory only>

**Human go/no-go decision:** ____________________  
**Reviewer and date:** ____________________

## 8. Evidence provenance

| Artifact | Path / task reference | Captured at |
| --- | --- | --- |
| Before-fix test log | <real path or NOT RUN — reason> | <timestamp or N/A> |
| After-fix test log | <real path or NOT RUN — reason> | <timestamp or N/A> |
| Before/after comparison | [comparison.md](comparison.md) | <timestamp> |
| Change brief | [change-brief.md](change-brief.md) | <timestamp> |
| Evaluation hints | <actual hints and task references, or N/A — no hints supplied; NOT VERIFIED if not recorded> | <timestamp or N/A> |
| Bob consumption screenshot | <path under ../../../bob_sessions/ or NOT VERIFIED — not yet captured> | <timestamp or N/A> |
```

Rules for variable rows: add one `AC-xx` row per agreed criterion; repeat the **same wording and IDs** in sections 1 and 7. Add zero to three `R-xx` rows; for zero risks use one `N/A — no risk identified within scope` row. Do not add new columns.

## 3. Exact `comparison.md` template

IBM Bob should materialize this as `changesafe/templates/comparison.md`. Its filled version is the presentation slide substitute: concise enough to show without live execution.

```markdown
# ChangeSafe Before / After — <change-title>

| Field | Value |
| --- | --- |
| Run ID | <run-id> |
| Goal | <same measurable goal as risk report> |
| Safety invariant | <same invariant as risk report, or N/A — no selected risk> |
| Before-fix state | <commit or labeled working-tree state> |
| After-fix state | <commit or labeled working-tree state, or N/A — no fix> |
| Project conventions | <link to reviewed rule; note approved deviations or N/A — none> |
| Change brief | [change-brief.md](change-brief.md) |
| Comparison captured at | <ISO-8601 timestamp> |

## Before / after results

| Measure | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test | <status and measured observation> | <status and measured observation> | <links to separate logs> |
| Relevant test suite | <tests run / failures / skipped> | <tests run / failures / skipped> | <links> |
| User-visible or domain behavior | <observed behavior> | <observed behavior> | <code/test link> |
| Execution time | <measured seconds or NOT VERIFIED> | <measured seconds or NOT VERIFIED> | <summary JSON links> |

## Goal check

| ID | Acceptance criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | <same text and ID as risk report> | MET / NOT MET / NOT VERIFIED | <actual result and link> |

**Overall goal:** MET / NOT MET / NOT VERIFIED — <same conclusion as risk report>

## What the evidence proves

<Two short sentences about the tested change and result. Do not generalize beyond the test.>

## Remaining risk

<One short paragraph; N/A — none identified within scope is allowed.>

## Presentation takeaway

<One sentence the presenter can read aloud.>
```

The overall-goal rule is deterministic: `NOT MET` if any required criterion is `NOT MET`; otherwise `NOT VERIFIED` if any required criterion is `NOT VERIFIED`; otherwise `MET`. Do not call this an automatic release approval. If no fix occurred, write `N/A — no fix` in after-fix cells and explain the outcome instead of manufacturing a red-to-green table.

## 4. Exact test-summary JSON shape

The deterministic script should create separate `before-test-summary.json` and `after-test-summary.json` files when those commands run. They use the same keys and value types. Unknown counts are `null`, never made-up zeros. The script may parse Maven Surefire XML or another reliable source; if it cannot establish a count, leave it `null`.

```json
{
  "schemaVersion": 1,
  "runId": "<run-id>",
  "phase": "before-fix",
  "capturedAt": "<ISO-8601 timestamp>",
  "sourceState": "<commit or labeled working-tree state>",
  "command": "<exact command>",
  "javaVersion": "<observed version or UNKNOWN>",
  "testProfile": "test",
  "result": "PASS",
  "exitCode": 0,
  "testsRun": null,
  "failures": null,
  "errors": null,
  "skipped": null,
  "durationSeconds": null,
  "logPath": "logs/before-test.log"
}
```

Allowed `phase`: `before-fix` or `after-fix`. Allowed `result`: `PASS`, `FAIL`, `UNKNOWN`. The displayed values above are **format examples, not real test results**. If a process exits successfully but no selected test actually ran, result must be `UNKNOWN`, not `PASS`. A summary JSON should be written only after a real command execution.

## 5. Exact `bob_sessions/` convention

The hackathon requires **all Bob task session consumption summary screenshots** in the repository-root `bob_sessions/` directory. The screenshots are actual Bob UI captures made by a team member; ChangeSafe cannot generate or simulate them. Use filenames `NN-<task-purpose>-consumption.png`, such as `01-plan-consumption.png`. Keep the original screenshots unaltered except a privacy-preserving crop/redaction if required; ensure the consumption numbers and task identity remain readable.

Optional `bob_sessions/INDEX.md` has this fixed layout:

```markdown
# IBM Bob Session Evidence

| No. | Bob task / session title or ID | Purpose | Consumption screenshot | Related artifact |
| --- | --- | --- | --- | --- |
| 01 | <actual title/ID> | Plan ChangeSafe | [01-plan-consumption.png](01-plan-consumption.png) | <link> |

**Completeness check:** <number of Bob task sessions used> task sessions / <number of real PNG files> consumption screenshots. Missing: <none or exact task IDs>.
```

This index is not a replacement for screenshots. Do not claim it is a Bob session export. If Bob exposes a native session export, retain it as additional evidence without changing its original format.

## 6. One-time documentation and configuration

- `changesafe/README.md` should use fixed headings: `What ChangeSafe Does`, `Prerequisites`, `Run ChangeSafe`, `Understand the Outputs`, `Add a Guardian`, `Safety and Limitations`. It is created once and updated when behavior changes, not regenerated for every run.
- `.bob/rules/01-project-conventions.md` is a **one-time, project-specific Bob workspace rule** drafted only after inspecting the repository and reviewed by a human before app-code generation. It is version-controlled, reused in later tasks, and kept short to avoid recurring context cost. Use this fixed layout:

```markdown
# Project Conventions for IBM Bob

> Status: DRAFT / APPROVED · Reviewed at: <ISO-8601 date or NOT REVIEWED> · Source state: <commit or labeled working tree>

## Authority and conflict handling
- Follow explicit task requirements and safety constraints first; then approved repository conventions; use generic best practices only where the project is silent.
- If sources conflict or evidence is insufficient, ask before changing application code. Do not treat an observed bug as a convention.

## Build and runtime
- <observed Java version, Maven command, test profile; source references>

## Architecture and package boundaries
- <observed domain/event/controller/data-access patterns; source references>

## Code and naming style
- <observed naming, object design, validation, formatting; source references>

## Tests and fixtures
- <observed framework, test location/naming, H2/fixture style; source references>

## UI or other relevant conventions
- <observed conventions only for touched areas, or N/A — not relevant>

## Unconfirmed or conflicting observations
- <specific uncertainty and files, or N/A — none>

## Approval
- Reviewed by: <human name or PENDING>
- Approved at: <timestamp or PENDING>
```

Only replace the placeholders with observations from multiple representative files or explicit project documentation. If a rule is uncertain, keep it in `Unconfirmed` rather than phrasing it as mandatory. Keep the file concise; do not dump the whole repo architecture into an always-loaded rule.
- `.bob/custom_modes.yaml` is Bob's native YAML mode configuration; `.bob/skills/changesafe/SKILL.md` is a concise workflow instruction file; `.bob/commands/changesafe.md` is a short entry point. Their structure must follow the installed Bob version's documentation. They are **not** run reports and must not contain invented results.

## 7. Validation before a report is called complete

Bob should check that every required heading is present, AC IDs/text match across both reports, statuses use the controlled vocabulary, relative evidence links resolve, every claimed test has a real log and summary, before/after files differ rather than overwrite one another, generated code follows the reviewed project rule or records an approved deviation, and `bob_sessions/` contains the team's actual consumption-summary screenshots for all used Bob tasks. If a screenshot or other evidence is missing, keep the report `NEEDS REVIEW` or `INCOMPLETE` and state exactly what the team must capture manually.

Also check AC IDs/text against `change-brief.md`, trace business decisions to confirmed sources, and verify that unresolved material questions did not silently become assumptions. Record actual approval rather than inventing it. A deterministic validator checks structure, statuses, links, and artifact presence; it cannot certify business correctness, real human approval, or screenshot authenticity.

## 8. Exact `change-brief.md` template

Materialize as `changesafe/templates/change-brief.md`, then fill `changesafe/evidence/<run-id>/change-brief.md` before implementation. Keep it concise. Draft ACs may remain provisional while clarification is pending; confirm them before dependent implementation. If approved criteria change, record the reason and retain the earlier version/source reference.

```markdown
# ChangeSafe Change Brief — <change-title>

| Field | Value |
| --- | --- |
| Run ID | <run-id> |
| Requester | <actual reference or NOT VERIFIED> |
| Requirement source | <actual task/document reference> |
| Source state | <commit or labeled working-tree state> |
| Status | DRAFT / AWAITING CLARIFICATION / READY FOR APPROVAL / APPROVED |

## 1. Problem and goal

**Problem:** <current pain or requested improvement; distinguish observation from hypothesis>  
**Goal:** <one measurable sentence>

## 2. Scope and exclusions

**In scope:** <components and behavior>  
**Out of scope:** <explicit exclusions or N/A — none>

## 3. Business rules and open questions

| Rule ID | Confirmed rule | Source / decision |
| --- | --- | --- |
| BR-001 | <confirmed rule, or NOT VERIFIED — not yet confirmed> | <register entry and source> |

| Question ID | Question for requester | Answer / status | Blocks |
| --- | --- | --- | --- |
| Q-01 | <simple business question, or N/A — none> | <answer and reference, or PENDING> | <affected implementation/test expectation, or N/A> |

## 4. Acceptance criteria

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | <precise observable condition> | <planned check> |

## 5. Implementation and verification plan

| Step | Planned action | Dependency / checkpoint |
| --- | --- | --- |
| 1 | <bounded action> | <approval or confirmed rule, or N/A> |

## 6. Decisions and approval

**Criteria revisions:** <change, reason, approver and previous source reference; or N/A — none>  
**Implementation approval:** <actual scope, requester, timestamp and task reference; or PENDING>  
**External-write authorization:** <actual commit/push/PR scope and reference; or NOT AUTHORIZED>
```

The brief is a lightweight PRD, not a mandatory lengthy product document. Rule and question rows may repeat; use one explicit N/A row when none applies. Analysis may proceed without implementation approval; the brief must not be labeled APPROVED unless the requester actually approved its relevant scope.

## 9. Exact shared business-rule register

Materialize `changesafe/templates/business-rules.md`, then create `changesafe/business-rules.md` with the same headings and empty tables. Placeholder rows below belong only to the template; do not put fictional confirmed decisions into the live register. Read relevant entries on demand rather than injecting the full register into every task.

```markdown
# ChangeSafe Business Rules

## 1. Rules and sources

| Rule ID | Business rule | Scope | Source | Status | Confirmed by | Confirmed at |
| --- | --- | --- | --- | --- | --- | --- |
| BR-001 | <precise rule> | <workflow/product scope> | <actual document or requester reference> | PROPOSED / CONFIRMED / SUPERSEDED | <actual reference or PENDING> | <ISO-8601 timestamp or PENDING> |

## 2. Open questions

| Question ID | Question | Scope | Status | Answer / decision reference |
| --- | --- | --- | --- | --- |
| Q-01 | <plain-language question> | <affected behavior> | OPEN / ANSWERED / WITHDRAWN | <actual answer/reference or PENDING> |

## 3. Decision history

| Decision ID | Rule / question IDs | Decision and reason | Source / confirmed by | Recorded at |
| --- | --- | --- | --- | --- |
| D-001 | <IDs> | <confirmed decision or explicit proposal> | <actual source and confirmation status> | <ISO-8601 timestamp> |
```

Use unique monotonically allocated IDs across the shared register; brief questions reference the same IDs. Never infer CONFIRMED solely from current code, tests, or an assistant's guess. Explicit requester confirmation or authoritative supplied business documentation with an identifiable source can support confirmation; ask when sources conflict. Preserve superseded decisions and history. Unknown confirmation dates stay PENDING, not fabricated timestamps. These files contain business decisions, not private personal data.

## 10. Exact optional PR description template

Materialize `changesafe/templates/pr-description.md`. Fill the run's `pr-description.md` only when needed; drafting it does not authorize commit, push, PR creation, merge, or deployment.

```markdown
# ChangeSafe PR — <change-title>

## Requirement and scope

<Goal, change-brief link, in-scope changes and exclusions.>

## Implementation and conventions

<Focused changes, approved project-rule references and any approved deviations.>

## Impact and risk

<Affected direct/downstream components, verified risks, and risk-report link.>

## Verification evidence

| Check | Before change | After change | Evidence |
| --- | --- | --- | --- |
| <test/check> | <actual result or NOT RUN — reason> | <actual result or NOT RUN — reason> | <links> |

## Goal and remaining risk

**Overall goal:** MET / NOT MET / NOT VERIFIED — <same conclusion as reports>  
**Remaining risk:** <unverified behavior and known limitations>  
**Comparison:** <comparison.md reference>

## Authorization and human review

**External-write authorization:** <actual scope and task reference or NOT AUTHORIZED>  
**Reviewer decision:** ____________________
```

Verify link destinations for the PR's rendering context: repo-relative Markdown links may need conversion to real repository URLs once commits exist. Do not invent PR links or claim a PR was created when only a description was drafted.

## 11. Execution grouping, retries, and scaffold validation

- Each phase's primary JSON/log describes one actual selected-test invocation. Use a combined targeted command where suitable; never merge counts from different commands while presenting them as one execution.
- Additional invocations use `executions/<execution-id>/test-summary.json` and `test.log`. The summary uses the same JSON keys and correct phase, command, source state and relative `logPath`. Reports link to the separate executions. Required primary files remain intact.
- A retry or further fix must never overwrite earlier executions. Create a fresh run directory for a new before/after pair, or retain additional executions under unique IDs and explicitly identify which results the reports compare. Preserve evidence of changed ACs and source states.
- New implementation-specific tests added only after development are after-only evidence, not fabricated before/after comparisons. Missing phase counts remain unknown or not run.
- Distinguish a regression of valid behavior, an unmet new requirement, a reproduced defect, and an environmental execution failure in report observations. Existing buggy behavior is not a required regression contract.
- Scaffold validation and demonstration evidence are separate. Validator tests use clearly labeled synthetic fixtures, never populate a real run with fictional passing data, and never generate mock Bob screenshots.
- Provide template validation (placeholders allowed) and completed-run validation (unresolved placeholders rejected, explicit missing-evidence statuses allowed). Validate all three run Markdown files, relevant register references, JSON types/statuses, and evidence links. A structurally valid incomplete report is not a COMPLETE report or proof that the goal was met.
- Mode/command discovery must be tested in the installed Bob IDE where possible. If unavailable, report it as NOT VERIFIED rather than assuming configuration files are functional.
