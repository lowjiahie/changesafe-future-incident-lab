# IBM Bob Assistance Evidence

> This file documents the IBM Bob IDE tasks that assisted in building and improving this
> repository. It is an organisational index only. It does not replace the mandatory
> consumption-screenshot PNGs in `bob_sessions/`; those must be captured manually from the
> Bob IDE task consumption panel after each task completes.

---

## Summary

| Bob task | Task ID | Bobcoins | Context fill | Outcome |
| --- | --- | --- | --- | --- |
| Task 01 — Plan ChangeSafe scaffold | `d039ddb47b0a5f8e54a8b873c4b61b22` | 7.48 | 9 % | [changesafe-plan.md](changesafe-plan.md) — full scaffold plan written and approved |
| Task 02 — Build ChangeSafe scaffold | `12db0cbc1bb316fb0c06603e45d0f7ea` | 15.61 | 46 % | All `.bob/`, `changesafe/`, and `bob_sessions/` files created |
| Task 03 — Checkout flow audit | `9b5771306d8cd2fdacf3c5b6880e5e99` | 18.80 | 78 % | [changesafe/evidence/checkout-flow-01/](changesafe/evidence/checkout-flow-01/) — full evidence pack |

**Total measured Bobcoins (Tasks 01–03):** 41.89

> Bobcoins are not tokens. IBM does not publish a fixed conversion ratio. Per-run cost is
> not predictable. All amounts above are read from the Bob IDE task consumption panel
> screenshots in `bob_sessions/`.

---

## Task 01 — Plan ChangeSafe scaffold

**Bob task ID:** `d039ddb47b0a5f8e54a8b873c4b61b22`  
**Bobcoins:** 7.48 · **Context fill:** 9 %  
**Consumption screenshot:** `bob_sessions/TeamLTY_task02_test_changesafe_workflow_correction.png`

### What Bob did

Bob operated in **Plan mode**. It read `CHANGESAFE_BOB_BUILD_BRIEF.md` and
`CHANGESAFE_OUTPUT_CONTRACT.md`, inspected the repository structure (README.md, pom.xml,
representative source and test files), identified supported Bob configuration features, and
produced a structured five-sub-task build plan.

### Artefacts produced by Bob

| Artefact | Path |
| --- | --- |
| Build plan | [`changesafe-plan.md`](changesafe-plan.md) |

### Human decisions recorded

- Team reviewed and approved the plan structure before implementation began.
- Sub-task 1 (project-conventions rule) was flagged as requiring human approval before
  any code generation could proceed.

---

## Task 02 — Build ChangeSafe scaffold

**Bob task ID:** `12db0cbc1bb316fb0c06603e45d0f7ea`  
**Bobcoins:** 15.61 · **Context fill:** 46 %  
**Consumption screenshot:** `bob_sessions/TeamLTY_task01_initial_changesafe_structure.png`

### What Bob did

Bob operated in **Agent mode**, implementing the approved `changesafe-plan.md` sub-tasks in
order. It inspected repository conventions, drafted the project-conventions rule, created all
Bob configuration files, materialised every Output Contract template verbatim, wrote the two
deterministic PowerShell scripts, and authored the developer README.

### Artefacts produced by Bob

| Artefact | Path |
| --- | --- |
| Project conventions rule (DRAFT) | [`.bob/rules/01-project-conventions.md`](.bob/rules/01-project-conventions.md) |
| ChangeSafe custom mode | [`.bob/custom_modes.yaml`](.bob/custom_modes.yaml) |
| ChangeSafe skill (12-phase workflow) | [`.bob/skills/changesafe/SKILL.md`](.bob/skills/changesafe/SKILL.md) |
| `/changesafe` command entry point | [`.bob/commands/changesafe.md`](.bob/commands/changesafe.md) |
| Impact-explorer Guardian spec | [`.bob/skills/changesafe/guardians/impact-explorer/GUARDIAN.md`](.bob/skills/changesafe/guardians/impact-explorer/GUARDIAN.md) |
| Test-gap-explorer Guardian spec | [`.bob/skills/changesafe/guardians/test-gap-explorer/GUARDIAN.md`](.bob/skills/changesafe/guardians/test-gap-explorer/GUARDIAN.md) |
| `.bobignore` | [`.bobignore`](.bobignore) |
| Risk report template | [`changesafe/templates/risk-report.md`](changesafe/templates/risk-report.md) |
| Comparison template | [`changesafe/templates/comparison.md`](changesafe/templates/comparison.md) |
| Change brief template | [`changesafe/templates/change-brief.md`](changesafe/templates/change-brief.md) |
| Business rules template | [`changesafe/templates/business-rules.md`](changesafe/templates/business-rules.md) |
| PR description template | [`changesafe/templates/pr-description.md`](changesafe/templates/pr-description.md) |
| Live business rules register (empty) | [`changesafe/business-rules.md`](changesafe/business-rules.md) |
| Test gate script | [`changesafe/scripts/run-targeted-tests.ps1`](changesafe/scripts/run-targeted-tests.ps1) |
| Output validator script | [`changesafe/scripts/validate-output.ps1`](changesafe/scripts/validate-output.ps1) |
| Developer README | [`changesafe/README.md`](changesafe/README.md) |
| Bob sessions index | [`bob_sessions/INDEX.md`](bob_sessions/INDEX.md) |

### Human decisions recorded

- Project-conventions rule reviewed; team approved it for use in subsequent tasks.
- No application code was changed during this task.

---

## Task 03 — Checkout flow audit (ChangeSafe demo run)

**Bob task ID:** `9b5771306d8cd2fdacf3c5b6880e5e99`  
**Bobcoins:** 18.80 · **Context fill:** 78 %  
**Consumption screenshot:** `bob_sessions/TeamLTY_task03_test_enhance_changesafe_workflow.png`

### What Bob did

Bob operated in **ChangeSafe mode**, executing the full 12-phase workflow on the prompt:
`/changesafe please help me check the checkout flow any possible issue and enhancement needed`.

Bob:
- Identified 10 risk hypotheses (R-01 through R-10) across the checkout flow, grounded in
  specific file and line references.
- Ran the deterministic test gate (before-fix and after-fix) using
  `changesafe/scripts/run-targeted-tests.ps1`.
- Asked the requester business clarification questions (Q-01 through Q-06) and recorded
  five confirmed business rules (BR-001 through BR-005).
- Implemented fixes for nine of the ten risks (R-07 deferred) after explicit human approval.
- Produced the full evidence pack: change-brief, risk-report, comparison, before/after test
  summaries, and Maven logs.
- Selected R-09 (client-supplied cart price never re-validated against catalog) as the primary
  safety contract; confirmed by a passing `PlaceOrderFromCartTest` after fix.

### Artefacts produced by Bob

| Artefact | Path |
| --- | --- |
| Change brief | [`changesafe/evidence/checkout-flow-01/change-brief.md`](changesafe/evidence/checkout-flow-01/change-brief.md) |
| Risk report | [`changesafe/evidence/checkout-flow-01/risk-report.md`](changesafe/evidence/checkout-flow-01/risk-report.md) |
| Before/after comparison | [`changesafe/evidence/checkout-flow-01/comparison.md`](changesafe/evidence/checkout-flow-01/comparison.md) |
| Before-fix test summary | [`changesafe/evidence/checkout-flow-01/before-fix-test-summary.json`](changesafe/evidence/checkout-flow-01/before-fix-test-summary.json) |
| After-fix test summary | [`changesafe/evidence/checkout-flow-01/after-fix-test-summary.json`](changesafe/evidence/checkout-flow-01/after-fix-test-summary.json) |
| Before-fix Maven log | `changesafe/evidence/checkout-flow-01/logs/before-fix-20260926-200012.log` |
| After-fix Maven log | `changesafe/evidence/checkout-flow-01/logs/after-fix-20260926-202817.log` |
| Confirmed business rules | [`changesafe/business-rules.md`](changesafe/business-rules.md) (BR-001 to BR-005, D-001 to D-005) |

### Application code changed by Bob (after explicit human approval)

| File | Risk addressed |
| --- | --- |
| `src/main/java/.../sales/order/jdbc/PlaceOrderJdbc.java` | R-04 — duplicate-order 500 error |
| `src/main/java/.../portal/web/OrderController.java` | R-01, R-02, R-03, R-10 |
| `src/main/java/.../portal/CheckoutOrder.java` | R-03, R-05 |
| `src/main/java/.../portal/PlaceOrderFromCart.java` | R-09 — catalog price re-validation |
| `src/main/java/.../portal/PortalConfig.java` | Wiring for R-05 and R-09 |
| `src/main/java/.../portal/web/CartIdFromCookies.java` | R-06 — cookie security flags |
| `src/main/java/.../sales/cart/jdbc/CartJdbc.java` | R-08 — stale cache |
| `src/main/resources/messages.properties` | R-10 — new error message key |
| `src/test/java/.../portal/PlaceOrderFromCartTest.java` | Safety contract test (R-09) |
| `src/test/java/.../portal/CheckoutOrderTest.java` | R-05 coverage |
| `src/test/java/.../portal/web/OrderControllerTest.java` | R-01, R-02, R-03, R-10 coverage |
| `src/test/java/.../portal/web/CartIdFromCookiesTest.java` | R-06 coverage |

### Human decisions recorded

- Risk fix decisions: R-01 to R-06, R-08 to R-10 → YES; R-07 → DEFER.
- Business rules Q-01 to Q-04 confirmed by requester; Q-05 resolved (Option A — portal layer);
  Q-06 deferred with R-07.
- Implementation approved by requester before any application code was edited.
- Report status: NEEDS REVIEW — five of eight ACs lack complete evidence; remaining risks
  documented in deferred backlog.

---

## How to update this file

After each new Bob task that assists with this repository:

1. Add a new row to the summary table at the top with the task ID, Bobcoin total, and
   context fill read from the Bob IDE task consumption panel.
2. Add a new section below the Task 03 section following the same structure:
   - What Bob did (mode, scope, key decisions made).
   - Artefacts produced or updated (table with path and type).
   - Human decisions recorded (approvals, confirmed business rules, deferral decisions).
3. Save a consumption screenshot to `bob_sessions/NN-<task-purpose>-consumption.png` and
   update [`bob_sessions/INDEX.md`](bob_sessions/INDEX.md).

**Never:**
- Invent task IDs, Bobcoin amounts, or screenshot references.
- Claim Bob produced an artefact it did not produce.
- Remove or edit human-decision records after the fact.
