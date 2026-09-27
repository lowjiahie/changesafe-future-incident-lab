# ChangeSafe Developer Guide

This guide explains how to run the ChangeSafe workflow in this repository, interpret its outputs,
extend it with a Guardian skill, and understand what it can and cannot do.

---

## What ChangeSafe Does

ChangeSafe is an evidence-first developer workflow powered by IBM Bob IDE. Given a proposed code
change or a Git diff, it helps a developer trace downstream failure paths, form testable risk
hypotheses, create or reuse executable regression checks, and collect a concise go/no-go evidence
pack -- **before release**.

The core idea is a **Future PreMortem**: imagine a production incident caused by the proposed
change before it ships. Every imagined incident must be grounded in source, requirements, a
failing test, or an explicitly stated uncertainty. The result is a chain of verifiable artifacts:

```
change/requirement
  -> affected workflow
  -> future-incident hypotheses
  -> executable checks
  -> test evidence
  -> human decision
```

ChangeSafe is a **Bob IDE workflow**, not a server or autonomous agent. It does not modify
application code until a human approves the proposed changes. The final go/no-go decision
always belongs to the reviewer.

---

## Prerequisites

- **Java 17** and Maven 3 (the `.\mvnw.cmd` wrapper is included).
- **IBM Bob IDE** with the ChangeSafe mode and skill installed (see `.bob/` directory).
- The project-conventions rule at `.bob/rules/01-project-conventions.md` reviewed and approved
  by the team. Bob uses this rule for all generated code; it must not be used for code generation
  while still marked DRAFT.
- Tests must pass locally using the H2 test profile before starting a ChangeSafe run:
  ```powershell
  .\mvnw.cmd clean test
  ```
- No MySQL connection is required for running tests; they use the in-memory H2 database.

> **IDE discovery note:** After saving the Bob configuration files, verify that the
> `ChangeSafe` mode appears in the Bob mode picker and that `/changesafe` appears in the
> command palette. File existence alone is insufficient -- if either does not appear, check
> `.bob/custom_modes.yaml` and `.bob/commands/changesafe.md` for syntax errors and confirm
> that the installed Bob version supports project-level modes and commands.

---

## Run ChangeSafe

### Step 1 — Open a Bob task in ChangeSafe mode

Select the **ChangeSafe** mode from the mode picker, or type `/changesafe` in any Bob task.
This activates the 12-phase workflow skill.

### Step 2 — Provide the change

Describe the proposed change, paste a diff reference, or use `@git-changes` to supply the
current Git diff. Be specific:

```
/changesafe
Review the checkout flow for potential problems with repeated order submissions.
Do not change application code until I approve.
```

### Step 3 — Follow the workflow

Bob will guide you through:

1. Clarifying the requirement and establishing a baseline source state.
2. Reusing the approved project-conventions rule (or drafting one for review if absent).
3. Identifying material business rules and asking you about any gaps.
4. Creating `changesafe/evidence/<run-id>/change-brief.md` with measurable acceptance criteria.
5. Running the cheap test gate (see below) and tracing downstream impact.
6. Proposing at most three risk hypotheses and selecting a safety invariant.
7. Running a before-fix baseline test and presenting findings for your approval.
8. **Waiting for your explicit approval** before any application-code edit.
9. After approval: implementing a focused fix, replaying tests, and evaluating each AC.
10. Delivering a filled `risk-report.md` and `comparison.md` for human review.

### Step 4 — Run the test gate script directly (optional)

You can run the test gate outside Bob for a deterministic check:

```powershell
# From the repository root:
.\changesafe\scripts\run-targeted-tests.ps1 `
    -RunId  checkout-retry-01 `
    -Tests  "OrderWorkFlowTest" `
    -Phase  before-fix
```

Output is saved to `changesafe/evidence/checkout-retry-01/logs/before-fix-<timestamp>.log`
and a JSON summary to `changesafe/evidence/checkout-retry-01/before-fix-test-summary.json`.

### Step 5 — Validate the output (optional)

After a run is complete, verify structural compliance:

```powershell
# Validate a completed run:
.\changesafe\scripts\validate-output.ps1 -RunDir "changesafe/evidence/checkout-retry-01"

# Validate the templates only (placeholders permitted):
.\changesafe\scripts\validate-output.ps1 -RunDir "changesafe/templates" -Template
```

### Step 6 — Capture Bob session screenshots

For the hackathon submission, open the Bob consumption summary for every task used and save
a screenshot to `bob_sessions/NN-<task-purpose>-consumption.png`. Update the index at
`bob_sessions/INDEX.md`. **ChangeSafe cannot generate these screenshots.**

---

## Understand the Outputs

Each ChangeSafe run produces an evidence directory:

```
changesafe/evidence/<run-id>/
    change-brief.md             Lightweight PRD: goal, ACs, business rules, plan
    risk-report.md              Full engineering report: impact, hypotheses, safety contract, goal check
    comparison.md               Concise before/after presentation artifact
    before-fix-test-summary.json  Structured test result (Output Contract §4 schema)
    after-fix-test-summary.json   (when a fix was implemented)
    logs/
        before-fix-<timestamp>.log   Full Maven output, unmodified
        after-fix-<timestamp>.log    (when a fix was implemented)
```

Shared files (not per-run):

| File | Purpose |
| --- | --- |
| `changesafe/business-rules.md` | Live register of confirmed business decisions |
| `changesafe/templates/` | Verbatim Output Contract templates (do not edit) |
| `.bob/rules/01-project-conventions.md` | Approved project conventions used by Bob |
| `bob_sessions/INDEX.md` | Index of hackathon consumption screenshots |

### Status vocabulary

| Context | Allowed values |
| --- | --- |
| Test result | `PASS`, `FAIL`, `UNKNOWN`, `NOT RUN` |
| Acceptance criterion | `MET`, `NOT MET`, `NOT VERIFIED` |
| Incident lifecycle | `HYPOTHESIS`, `EVIDENCE-BACKED`, `REPRODUCED`, `PREVENTED`, `CONTAINED`, `UNRESOLVED` |
| Overall goal | `MET`, `NOT MET`, `NOT VERIFIED` |
| Report status | `COMPLETE`, `NEEDS REVIEW`, `INCOMPLETE` |

A claim of `PREVENTED` requires a reproduced pre-fix failure **and** a passing post-fix replay.
`MET` requires evidence for the corresponding criterion. `NOT VERIFIED` means the evidence is
missing, not that the criterion failed.

---

## Add a Guardian

A Guardian is a narrow, domain-specific ChangeSafe extension. It activates only when the proposed
change matches its trigger domain -- the coordinator does not run every Guardian by default.

To add a Guardian:

1. Create a directory: `.bob/skills/changesafe/guardians/<guardian-name>/`
2. Write `GUARDIAN.md` with these required sections:
   - **Trigger condition:** the precise signal that activates this Guardian
     (e.g., "any change to `billing/payment/` or `PaymentCollected` event consumers").
   - **Read permissions:** which files or packages this Guardian may read.
   - **Edit permissions:** `none` (read-only) or specific authorized paths.
   - **Evidence output format:** the exact fields this Guardian reports back to the coordinator.
   - **Cost / stop condition:** the maximum context or Bobcoins this Guardian may consume
     before returning a partial result and stopping.
3. Register the Guardian in `.bob/skills/changesafe/SKILL.md` by adding a routing rule in the
   Phase 5 (Impact analysis) section: "if change touches <domain>, activate <guardian-name>
   Guardian and include its evidence in the impact map."

**Implemented Guardians:**

| Guardian | Trigger | Output sections |
| --- | --- | --- |
| `database-migration` | Change touches `schema.sql`, `application*.properties` datasource-init keys, or any `src/main/resources/**/*.sql` file | Schema change classification; upgrade impact; migration SQL recommendation; data safety questions |

**Future Guardian examples (not yet implemented):** `payment-integrity`, `api-compatibility`.

---

## Safety and Limitations

- **Human approval required** for all application-code edits and commands that modify data.
  Bob will not edit the Java application until you explicitly approve the proposed changes.
- **Reports are advisory.** The risk report offers a `Bob advisory recommendation` field, clearly
  labeled as advisory. The final go/no-go decision is always filled by a human reviewer.
- **The validator checks structure, not business correctness.** `validate-output.ps1` confirms
  that headings, status vocabulary, AC-ID consistency, and relative links are correct. It cannot
  verify whether test results match real executions, whether a human actually approved anything,
  or whether screenshots are genuine.
- **Screenshots are manually captured.** The `bob_sessions/` directory must be populated by a
  team member opening the Bob consumption summary UI and saving a screenshot. ChangeSafe has no
  mechanism to generate or simulate those images.
- **No external service calls.** The test gate script runs Maven locally. No test output is sent
  to any external endpoint.
- **PR creation requires explicit authorization.** The PR description template may be drafted
  during a run, but no commit, push, or PR is created unless you explicitly authorize it.
- **Test results do not constitute release approval.** A PASS result means the selected tests
  passed under the H2 test profile. It does not mean all possible failures have been eliminated
  or that the change is safe for production.
- **The project-conventions rule must be approved before code generation.** Until the rule is
  changed from DRAFT to APPROVED, Bob will not generate Java code or tests for this repository.

---

## Attribution

The sample application used by this experiment is based on
[ttulka/ddd-example-ecommerce](https://github.com/ttulka/ddd-example-ecommerce) by Tomas Tulka,
licensed under the [MIT License](../LICENSE). The ChangeSafe workflow, templates, scripts, and
documentation in this directory are the team's work for the IBM Bob 2.0 Hackathon and are not
an official version of the upstream project.

---

## Bobcoin budget notes

### Measured session costs (this project)

| Task | Bob task ID | Bobcoins | Context fill | Primary cost driver |
| --- | --- | --- | --- | --- |
| Plan / build spec read | `d039ddb47b0a5f8e54a8b873c4b61b22` | 7.48 | 9 % | Planning-only; low context. Non-recurring. |
| Initial scaffold build | `12db0cbc1bb316fb0c06603e45d0f7ea` | 15.61 | 46 % | One-time: two large spec docs + full project inspection + all templates and scripts. Non-recurring for this project. |
| Checkout flow audit | `9b5771306d8cd2fdacf3c5b6880e5e99` | 18.80 | 78 % | Broad 10-risk investigation; both guardians fired; no early-stop applied; full evidence pack written in-context. Representative repeat-run high. |

These are the only three sessions with recorded measurements. All numbers are read from the
Bob IDE task consumption panel screenshots in `bob_sessions/`. Do not add rows without a
corresponding screenshot.

### Budget monitoring (manual)

There is no automatic Bobcoin meter. Read the Bob IDE task consumption panel manually at the
three checkpoints built into the ChangeSafe skill:

1. **Intake** — before starting Phase 2. Record the reading as `Budget at intake` in the
   change-brief header. If balance ≤ `RESERVE_THRESHOLD` (default 5 Bobcoins), stop and
   deliver a minimal scoped report.
2. **Post-investigation** — at the end of Phase 5. If balance ≤ threshold or the optional
   `RUN_CAP` has been reached, pause and ask the requester before continuing.
3. **Pre-fix** — at the end of Phase 7, before writing application code. Same check.

The `RESERVE_THRESHOLD` defaults to 5 Bobcoins remaining. The `RUN_CAP` is optional and
set by the requester at intake; if not set, only the threshold is enforced.

### Important caveats

- **Bobcoins ≠ tokens.** IBM does not publish a fixed conversion ratio. Do not assume a
  fixed cost per run or per token.
- **Per-run cost is not predictable.** Cost depends on context fill, file sizes, number of
  guardians, and model behaviour. The 18.80-coin run (78 % fill) is an observed upper bound
  for a broad audit with no early-stop; a focused, two-file change should cost significantly
  less, but this is an estimate, not a guarantee.
- **Efficiency gates are instructions, not enforced limits.** The convention-reuse gate,
  complexity gate, and risk-count guard in the skill reduce context use by instruction. They
  rely on the AI following those instructions; there is no hard technical cap.
