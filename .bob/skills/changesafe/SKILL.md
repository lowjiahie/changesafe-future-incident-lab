---
name: changesafe
description: >-
  Use when the user wants to analyze a proposed code change, diff, or requirement for downstream
  failure risks before release. Activates the ChangeSafe Future Incident Lab workflow: impact
  analysis, risk hypotheses, safety contracts, executable regression checks, and evidence pack.
  Also invoked by /changesafe.
metadata:
  argument-hint: "[change description or diff reference]"
---

# ChangeSafe Skill — Future Incident Lab

Run this 12-phase workflow for every ChangeSafe request. Follow the phases in order.
Business clarification is a cross-cutting checkpoint: it can interrupt any phase.
Pause dependent implementation while answers are pending; safe independent analysis may continue.

## Token-efficiency rules (apply throughout)

**Budget constants (configurable per-run):**
- `RESERVE_THRESHOLD` — default **5 Bobcoins remaining**. At any budget checkpoint, if the
  remaining balance falls at or below this value, deliver a minimal honest report and stop.
  The requester may raise or lower this default at intake.
- `RUN_CAP` — optional per-run spending cap in Bobcoins, set by the requester at intake.
  If cumulative spend since Phase 1 reaches this cap, **pause and ask the requester** before
  continuing. No default; if not set, only `RESERVE_THRESHOLD` is enforced.

**There is no automatic Bobcoin meter.** The Bob IDE task consumption panel must be read
manually. Record the reading in the change-brief header as `Budget at intake` at Phase 1,
and re-read it at the three checkpoints below (end of Phase 5, end of Phase 7, end of
Phase 10). Do not assume the tool can read its own mid-task consumption automatically.

- **Cheap intake first.** Use `git diff --name-only`, targeted grep, and local tests to narrow
  scope before reading large files or spawning subagents.
- **Test script before deep reasoning.** Run `changesafe/scripts/run-targeted-tests.ps1` as a
  deterministic filter before any in-depth investigation. Pass the full raw output to a log;
  expose only the short summary to context.
- **Two-pass analysis.** First produce a small change summary and examine the test-gate result.
  If it is a local, low-risk edit with adequate passing coverage, stop with a short report.
  A stop is a legitimate ChangeSafe result.
- **Minimal context packets.** Each subagent gets only: change summary, bounded question,
  relevant file paths/diff. Do not pass full conversation history.
- **Conditional parallelism.** Zero subagents for a simple change; one explore subagent for a
  focused cross-module question; two only when questions are truly independent.
- **Bound outputs.** Enumerate all risks from code evidence (no cap). Select the highest-value
  YES risk for an executable check. Run targeted Maven tests before the full suite. Any risks
  beyond the top 3 tracked actively are summarised as one-line backlog rows (see Phase 6a).
- **Meter Bobcoins.** Read the Bob IDE panel at each checkpoint (Phase 5, Phase 7, Phase 10).
  If balance ≤ `RESERVE_THRESHOLD` or `RUN_CAP` reached, stop after an honest report.

---

## Phase 1 — Receive requirement

1. Establish the goal, scope, and comparison source state (commit hash, diff, or labeled
   working-tree state).
2. If the request is too broad, ask one focused clarifying question before continuing.
3. Capture the source state: run `git rev-parse HEAD` and `git status --short`.

**Budget intake checkpoint (mandatory — do before Phase 2):**
1. Ask the requester: "What is your per-run spending cap for this ChangeSafe task?
   (Optional — leave blank to use only the default reserve threshold of 5 Bobcoins.)"
   Record the answer as `RUN_CAP` in the change-brief header (`N/A` if not set).
2. Remind the requester to read the current Bobcoin balance from the Bob IDE task
   consumption panel. Record the reading as `Budget at intake` in the change-brief header.
3. If remaining balance ≤ `RESERVE_THRESHOLD` (default 5): deliver a one-paragraph scope
   summary, mark the change-brief status `INCOMPLETE — BUDGET EXHAUSTED`, and stop.
   This is a valid ChangeSafe result.

---

## Phase 2 — Understand project and conventions

1. Check for `.bob/rules/01-project-conventions.md`.
   - If it exists and is APPROVED: load it.
     **Convention reuse gate:** If the rule's recorded source-state commit matches the
     current HEAD (run `git rev-parse HEAD` and compare), and no convention-relevant file
     has changed since that commit (check: `git diff <rule-commit> HEAD -- pom.xml README.md
     src/main/java src/test/java`), skip all repository-wide exploration entirely. Record
     "Conventions reused from commit `<hash>`" in the change-brief and proceed to Phase 3.
     Do not re-read `README.md`, `pom.xml`, or representative source files during the
     reuse path.
   - If it is DRAFT: remind the user it needs approval before app-code generation. Proceed with
     analysis; do not generate Java code yet.
   - If it does not exist: run the first-encounter onboarding pass.
     Issue the following four reads as a **single concurrent tool-call batch** (not
     subagents — these are bounded read-only files; subagents are not warranted here
     and would count against the two-guardian-per-run limit):
     - `README.md`
     - `pom.xml`
     - Representative source file(s) (e.g. a JDBC use-case implementation and a domain
       interface from the bounded context most relevant to the proposed change)
     - Representative test file(s) (e.g. a `@JdbcTest` unit test and an integration test
       that exercises a comparable workflow)

     After all four reads complete, reconcile the observations: identify any conflicts
     between what the README describes and what the source code shows (naming conventions,
     package structure, test patterns, dependency rules). Surface conflicts explicitly.
     Draft the conventions rule from the reconciled observations and present it to the
     requester for approval. **Do not adopt the draft rule for convention checking or code
     generation until the requester explicitly approves it.**
2. Inspect `git status` to identify uncommitted user changes. Do not overwrite or claim them.
3. Re-check the relevant convention against files touched by the proposed change.

---

## Phase 3 — Understand business rules and clarify

1. Read confirmed entries from `changesafe/business-rules.md` that are relevant to this change.
   Do not load the full register into context -- read specific rule IDs on demand.
2. If the user supplied a document (ticket, PDF, DOCX, spec), extract exact acceptance criteria
   and unresolved questions before the risk analysis; cite the source in the report.
3. Identify material missing or conflicting business rules. Ask the requester in simple language.
   Never silently decide: stock reservation timing, backorders, partial checkout, delivery
   requirements, or submission identity.
4. Record new confirmed decisions in `changesafe/business-rules.md` with the decision source and
   confirmation reference.

---

## Phase 4 — Plan and lightweight PRD

1. Create the evidence directory: `changesafe/evidence/<run-id>/` (lowercase kebab-case,
   e.g. `checkout-retry-01`). Never overwrite a prior run directory.
2. Copy `changesafe/templates/change-brief.md` to `changesafe/evidence/<run-id>/change-brief.md`.
3. Fill in: run ID, requester, requirement source, source state, problem, goal, scope, business
   rules, open questions, acceptance criteria (AC-01, AC-02, ...), and implementation plan.
4. Small changes require a short brief. Mark status DRAFT or AWAITING CLARIFICATION as appropriate.
5. Draft ACs may remain provisional while clarification is pending. Do not mark APPROVED unless
   the requester explicitly approves the relevant scope.

---

## Phase 5 — Change-impact analysis

1. Use `git diff --name-only` and targeted grep to identify directly changed files and their
   callers/dependencies.
2. Trace the call chain, event flow, persistence, and downstream components -- including unedited
   code that may be affected.
3. **Complexity gate:** If the proposed change touches ≤ 2 files AND a targeted grep finds no
   references to those files from packages outside their bounded context, skip the
   impact-explorer guardian. Record "Local change — no cross-module guardian spawned" in the
   change-brief impact section and proceed to step 4.
   Otherwise, if the change crosses module boundaries and the question is non-trivial, select
   the appropriate guardian based on the layers touched:
   - **Single-layer change** (all changed files fall within one layer pattern — `rest`,
     `jdbc`, `listeners`, `domain`, or `portal`): spawn one read-only `explore` subagent
     using the guardian spec at
     `.bob/skills/changesafe/guardians/impact-explorer/GUARDIAN.md`.
     Pass only the fields defined in that spec's **Input packet** table.
   - **Multi-layer change** (changed files span ≥ 2 distinct layer patterns): spawn one
     read-only `explore` subagent using the guardian spec at
     `.bob/skills/changesafe/guardians/parallel-layer-explorer/GUARDIAN.md`.
     Pass only the fields defined in that spec's **Input packet** table, including the
     `layers_detected` field listing each identified layer label.
     This guardian investigates each layer concurrently using parallel tool calls within
     its single invocation and counts as one guardian slot.
4. **Database-migration routing:** If the proposed change touches `schema.sql`, any
   `application*.properties` datasource-init key (`spring.sql.init.*`, `spring.datasource.*`),
   or any `src/main/resources/**/*.sql` file, activate the `database-migration` guardian using
   the spec at `.bob/skills/changesafe/guardians/database-migration/GUARDIAN.md`.
   Pass only the fields defined in that spec's **Input packet** table.
   Record the guardian's output in the change-impact map under component "database schema".
   This counts against the two-guardian-per-run limit.
5. Populate the impact map table in the risk report (section 3).

**Budget checkpoint:** Read the Bob IDE task consumption panel. If remaining balance ≤
`RESERVE_THRESHOLD` or cumulative spend has reached `RUN_CAP`, pause and ask the requester
for approval before continuing to Phase 6.

---

## Phase 6 — Predict potential failures, enumerate all risks, and select safety contracts

### 6a — Exhaustive risk enumeration (mandatory)

1. Enumerate **all** concrete risks identifiable in the affected flow from code evidence.
   Do not cap the list. Use sequential IDs: R-01, R-02, R-03, R-04, … as needed.

   **Risk-count guard:** After enumeration, select the top 3 risks by severity for active
   tracking in this run. Risks ranked 4 and below are recorded in the deferred risk backlog
   (Section 7 of the risk report) as one-line summary rows only — they are not expanded to
   full evidence rows in context. All risks remain visible in the report; the guard reduces
   in-context token use for broad audits. If fewer than 4 risks are identified, the guard
   does not apply.
2. For each risk state:
   - **Trigger and failure chain** — the exact code path or condition that causes it.
   - **User / system impact** — what the user or operator experiences.
   - **Evidence** — specific file, line, or test reference; never generic advice.
   - **Confidence** — HIGH / MEDIUM / LOW based on code evidence, not intuition.
   - **Initial status** — HYPOTHESIS (inferred) or EVIDENCE-BACKED (confirmed by test or log).
   - **Severity** — CRITICAL / HIGH / MEDIUM / LOW using the scale below.

   | Severity | Meaning |
   | --- | --- |
   | CRITICAL | Data loss, double-charge, security breach, or irreversible production damage |
   | HIGH | Visible user error, broken workflow, significant data inconsistency |
   | MEDIUM | Degraded experience, edge-case failure, recoverable inconsistency |
   | LOW | Minor UX issue, cosmetic defect, unlikely edge case |

3. If test coverage gaps are unclear AND this question is independent of the impact exploration,
   spawn a second read-only `explore` subagent using the guardian spec at
   `.bob/skills/changesafe/guardians/test-gap-explorer/GUARDIAN.md`.
   Pass only the fields defined in that spec's **Input packet** table.
   Maximum two guardians per run total.

### 6b — User selection gate (mandatory human checkpoint)

4. Present the complete risk table to the requester. For each risk ask:
   - **Fix in this run?** (YES / DEFER / WONT-FIX)
   - **Why deferred?** (optional note — capacity, scope, dependency, etc.)

   Do not select risks on behalf of the user. Do not proceed to 6c until the user has
   responded with explicit per-risk decisions.

5. Record the user's decisions in the change-brief and in the risk table:
   - Risks marked YES → enter Phase 7–11 workflow for this run.
   - Risks marked DEFER → status set to DEFERRED; carried to the risk backlog in the report
     (Section 7, "Deferred risk backlog") and to `changesafe/business-rules.md` as open questions.
   - Risks marked WONT-FIX → status set to WONT-FIX with the stated reason.

### 6c — Safety contract selection

6. From the YES risks only, select the single highest-value risk for the safety contract.
   Document the selection rationale (why this risk over the others being fixed in this run).
7. Define the safety invariant: one precise, testable condition that must hold after the fix
   (e.g., "the same checkout attempt creates no more than one order under any retry scenario").

---

## Phase 7 — Generate or reuse tests and run before-change baseline

> **Budget checkpoint:** Read the Bob IDE task consumption panel before running tests.
> If remaining balance ≤ `RESERVE_THRESHOLD` or `RUN_CAP` reached, pause and ask the
> requester before continuing.

1. Identify existing tests that cover the affected path. Reuse valid-behavior tests where possible.
2. Propose acceptance and adverse-condition checks for the selected safety invariant.
3. Before changing application behavior, run the test gate:
   ```
   changesafe/scripts/run-targeted-tests.ps1 -RunId <run-id> -Tests "<TestClass>" -Phase before-fix
   ```
4. The script saves a full log to `changesafe/evidence/<run-id>/logs/` and writes
   `changesafe/evidence/<run-id>/before-test-summary.json`.
5. Classify failures as: pre-existing failures, unmet new requirements, reproduced defects,
   or environment issues. Do not treat a pre-existing failure as a new finding.
6. If reliable evidence is unavailable (compile error, environment failure): resolve or narrow
   scope transparently. Do not continue with fabricated results.
7. A cheap existing-test gate may run earlier once scope is clear; it does not replace this
   comparable baseline.

---

## Phase 8 — Present proposed changes for approval checkpoint

This is a mandatory human checkpoint. Do not edit application code before this step completes.

1. Present: the change-brief, impact map, the **complete risk table** (all risks with Severity,
   Confidence, and user decisions YES/DEFER/WONT-FIX), selected safety invariant, proposed
   test additions, and proposed application code changes (if any).
2. Await an explicit approval, revision request, or rejection.

**If NOT approved:** Deliver an analysis-only report. Fill the risk report as far as evidence
allows. Mark implementation approval as PENDING and app-code edits as NOT AUTHORIZED.
Stop here -- this is a valid ChangeSafe result, not a failure.

**If REVISE requested:** Return to Phase 4 (update change-brief.md), re-examine ACs and scope,
and present again. Do not silently narrow or expand scope.

**If approved:** Proceed to develop focused changes. Implement only the authorized scope.
Follow the approved project conventions rule. Report which conventions were applied and call out
any deviations explicitly.

---

## Develop focused changes

(Runs only after Phase 8 approval.)

Implement the minimal authorized fix following project conventions. Separate intentional user
purchases from replay of a single submission intent. Do not add features, abstractions, or
refactors beyond the authorized scope.

---

## Phase 9 — Replay and complete coverage

1. Rerun the same test command and settings used in Phase 7:
   ```
   changesafe/scripts/run-targeted-tests.ps1 -RunId <run-id> -Tests "<TestClass>" -Phase after-fix
   ```
2. Add implementation-specific tests where the fix introduces new behavior.
3. Never weaken expectations to get green tests. Stop and report blockers instead of looping.

**New business ambiguity gate:** If new material business uncertainty emerges during testing,
return to Phase 3, ask the requester, and record confirmed decisions before continuing.

**Required-checks gate:** If tests fail for technical reasons, return to development for the
specific failure. If tests fail for a newly material business question, pause and clarify first.
Loop only for bounded, identifiable issues -- do not loop indefinitely.

---

## Phase 10 — Review and evaluate acceptance criteria

1. Run Bob's built-in `/review` on the actual diff if available. Carry relevant findings into
   the evidence report.
2. Evaluate each AC as MET, NOT MET, or NOT VERIFIED using actual evidence (not test success alone).
3. Apply the deterministic goal rule:
   - NOT MET if any required criterion is NOT MET.
   - NOT VERIFIED if any required criterion is NOT VERIFIED.
   - MET only if all required criteria are MET with evidence.
4. Test success does not establish complete requirement coverage, automatic release approval,
   or absence of production incidents.

**Rework gate:** If rework is needed for a technical failure, return to development.
If rework requires a scope change, return to Phase 4 and revise the change brief.

**Budget checkpoint:** Read the Bob IDE task consumption panel. If remaining balance ≤
`RESERVE_THRESHOLD` or `RUN_CAP` reached, pause and ask the requester before continuing
to Phase 11.

---

## Phase 11 — Deliver evidence pack and summary

**Regeneration guard:** If a prior run's `change-brief.md` or `risk-report.md` already exists
in `changesafe/evidence/<run-id>/` and the relevant source files have not changed since that
run's recorded source state, do not regenerate those files from scratch. Update only the
sections that changed. If the validator passes on the existing files after updates, stop —
do not re-render unchanged content.

1. Fill `changesafe/evidence/<run-id>/risk-report.md` from `changesafe/templates/risk-report.md`.
   Keep all required headings in order. Use controlled vocabulary only.
2. Fill `changesafe/evidence/<run-id>/comparison.md` from `changesafe/templates/comparison.md`.
3. Validate the run directory:
   ```
   changesafe/scripts/validate-output.ps1 -RunDir "changesafe/evidence/<run-id>"
   ```
4. If the validator reports failures, fix the structural issues before calling the report complete.
5. Remind the team to capture the Bob task consumption screenshot manually and save it under
   `bob_sessions/NN-<task-purpose>-consumption.png`. Update `bob_sessions/INDEX.md`.

**If no PR authorization:** Hand off locally with remaining risks clearly documented in the
report's "Remaining risk (in-scope, not fixed)" field and the "Deferred risk backlog" table in
Section 7. This is the normal outcome. Each deferred risk must have a suggested follow-on run ID.

**If explicit PR authorization was granted:** Proceed to Phase 12.

---

## Phase 12 — Commit, push, and create PR (only with explicit authorization)

1. Confirm the exact scope and reference of the authorization.
2. Copy `changesafe/templates/pr-description.md` to `changesafe/evidence/<run-id>/pr-description.md`
   and fill it.
3. Commit, push, and create the PR using configured Git tooling and the filled PR description.
4. Human review and merge decision remain with the team. ChangeSafe does not approve merges.

---

## Templates and scripts reference

| Purpose | Path |
| --- | --- |
| Change brief template | `changesafe/templates/change-brief.md` |
| Risk report template | `changesafe/templates/risk-report.md` |
| Comparison template | `changesafe/templates/comparison.md` |
| Business rules template | `changesafe/templates/business-rules.md` |
| PR description template | `changesafe/templates/pr-description.md` |
| Live business rules register | `changesafe/business-rules.md` |
| Test gate script | `changesafe/scripts/run-targeted-tests.ps1` |
| Output validator | `changesafe/scripts/validate-output.ps1` |
| Project conventions rule | `.bob/rules/01-project-conventions.md` |
| Evidence directories | `changesafe/evidence/<run-id>/` |
| Bob session screenshots | `bob_sessions/` |
