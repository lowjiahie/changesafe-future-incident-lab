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
- **Bound outputs.** At most three risk hypotheses. Select the highest-value one for an
  executable check. Run targeted Maven tests before the full suite.
- **Meter Bobcoins.** Check remaining budget at intake, post-investigation, and pre-fix.
  If low, stop after an honest report.

---

## Phase 1 — Receive requirement

1. Establish the goal, scope, and comparison source state (commit hash, diff, or labeled
   working-tree state).
2. If the request is too broad, ask one focused clarifying question before continuing.
3. Capture the source state: run `git rev-parse HEAD` and `git status --short`.

---

## Phase 2 — Understand project and conventions

1. Check for `.bob/rules/01-project-conventions.md`.
   - If it exists and is APPROVED: load it. Skip repeated repository-wide exploration.
   - If it is DRAFT: remind the user it needs approval before app-code generation. Proceed with
     analysis; do not generate Java code yet.
   - If it does not exist: run the first-encounter onboarding pass (read README.md, pom.xml,
     representative source and test files) and draft the rule for review before continuing.
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
3. If the change crosses module boundaries and the question is non-trivial, spawn one read-only
   `explore` subagent with a minimal context packet: change summary, bounded question, relevant
   file paths. The subagent must return: relevant file paths, event sequence, one concrete failure
   possibility, and unknowns. It must not edit.
4. Populate the impact map table in the risk report (section 3).

---

## Phase 6 — Predict potential failures and select safety contracts

1. Identify at most three concrete risk hypotheses grounded in code evidence, not generic advice.
   Use R-01, R-02, R-03 IDs.
2. For each hypothesis: state the trigger and failure chain, user/system impact, evidence links,
   confidence (HIGH/MEDIUM/LOW), and initial status (HYPOTHESIS or EVIDENCE-BACKED).
3. If test coverage gaps are unclear AND this question is independent of the impact exploration,
   spawn a second read-only `explore` subagent. Maximum two subagents per run total.
4. Select the single highest-value hypothesis for the safety contract. Document the selection
   rationale.
5. Define the safety invariant: one precise, testable condition (e.g., "the same checkout
   attempt creates no more than one order under any retry scenario").

---

## Phase 7 — Generate or reuse tests and run before-change baseline

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

1. Present: the change-brief, impact map, risk hypotheses, selected safety invariant, proposed
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

---

## Phase 11 — Deliver evidence pack and summary

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
report's "Remaining risk" and "Human go/no-go decision" fields. This is the normal outcome.

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
