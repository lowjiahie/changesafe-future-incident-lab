# Plan — Checkout Idempotency Protection

## Top-Level Overview

**Goal:** Prevent duplicate orders and charges when a user double-clicks the checkout button
or the browser retries the POST to `/order`. The solution adds a client-generated idempotency
key to the checkout form. The server stores it on the order and, on any repeat submission with
the same key, returns the already-placed order instead of creating a new one.

**Run ID:** `checkout-idempotency-01`

**ChangeSafe phase mapping — ALL phases are mandatory.**
Implementation sub-tasks (ST-3 through ST-7) may not begin before ST-2 (Phase 4–8 gate) is
complete and explicitly approved. Evidence sub-tasks (ST-8 through ST-10) may not be skipped
or delegated to subagents without explicit file-writing instructions.

```
ST-1  Phase 1–3   Receive, understand, clarify, record business rules
ST-2  Phase 4–8   Create evidence dir + change-brief → before-fix test gate → Phase 8 approval checkpoint
ST-3  Phases dev  Sub-task: Database schema
ST-4  Phases dev  Sub-task: Domain layer
ST-5  Phases dev  Sub-task: Portal layer
ST-6  Phases dev  Sub-task: Web layer
ST-7  Phase 9     Sub-task: Tests + after-fix test gate (run-targeted-tests.ps1)
ST-8  Phase 10    Evaluate acceptance criteria
ST-9  Phase 11    Fill risk-report.md + comparison.md + run validate-output.ps1
ST-10 Phase 12    (only if PR explicitly authorized)
```

**Out of scope:** Payments deduplication, API/JSON endpoints, distributed locking, async event
deduplication.

---

## ST-1 — ChangeSafe Phases 1–3: Receive, understand, clarify

**Intent:** Establish the source state, load conventions, confirm all business rules before any
code or evidence file is written. Record confirmed decisions in `changesafe/business-rules.md`.

**Expected Outcomes:**
- `git rev-parse HEAD` and `git status --short` captured.
- `.bob/rules/01-project-conventions.md` status checked (DRAFT / APPROVED).
- `changesafe/business-rules.md` updated with BR-001 through BR-004.
- All clarifying questions answered before proceeding to ST-2.

**Todo List:**
- [ ] Run `git rev-parse HEAD` and `git status --short`; record both in context.
- [ ] Check `.bob/rules/01-project-conventions.md` status; note DRAFT/APPROVED in context.
- [ ] Read relevant entries from `changesafe/business-rules.md`.
- [ ] Ask any unresolved business questions (idempotency key lifetime, duplicate response, nullable column).
- [ ] Write confirmed decisions to `changesafe/business-rules.md` (BR-001 through BR-004).

**Relevant Context:**
- [`changesafe/business-rules.md`](changesafe/business-rules.md)
- [`.bob/rules/01-project-conventions.md`](.bob/rules/01-project-conventions.md)

**Status:** [x] done

---

## ST-2 — ChangeSafe Phases 4–8: Evidence scaffold + before-fix gate + approval checkpoint

**⛔ GATE: No implementation code may be written until this sub-task is fully complete and
the human has given explicit approval at the Phase 8 checkpoint.**

**Intent:** Create the evidence directory, fill the change-brief, capture the before-fix test
baseline using the canonical script (not bare Maven), then present the full analysis for human
approval before any application code is touched.

**Expected Outcomes:**
- `changesafe/evidence/checkout-idempotency-01/` directory exists.
- `changesafe/evidence/checkout-idempotency-01/change-brief.md` filled (status: DRAFT or
  AWAITING CLARIFICATION until approved).
- Before-fix test gate run via `run-targeted-tests.ps1`; `before-fix-test-summary.json` written
  to the evidence directory.
- Impact map (Section 3 of risk-report) drafted in context.
- **Complete risk table** drafted in context — ALL identifiable risks with Severity, Confidence,
  initial status, and user's YES/DEFER/WONT-FIX decision per risk.
- DEFERRED risks recorded in `changesafe/business-rules.md` as open questions.
- Safety invariant selected from YES risks only, and documented.
- Human reads and explicitly approves (or revises/rejects) before any code change.

**Todo List:**
- [ ] Create directory `changesafe/evidence/checkout-idempotency-01/`.
- [ ] Copy `changesafe/templates/change-brief.md` to
  `changesafe/evidence/checkout-idempotency-01/change-brief.md`.
- [ ] Fill change-brief: run ID, requester, requirement source, source state, problem, goal,
  scope, business rules (BR-001–BR-004), questions (Q-01–Q-03), acceptance criteria
  (AC-01–AC-04), implementation steps, approval status DRAFT.
- [ ] Run the before-fix test gate **using the script, not bare Maven**:
  ```
  changesafe/scripts/run-targeted-tests.ps1 -RunId checkout-idempotency-01 \
      -Tests "PlaceOrderTest,OrderTest,OrderWorkFlowTest" -Phase before-fix
  ```
- [ ] Confirm `changesafe/evidence/checkout-idempotency-01/before-fix-test-summary.json` was
  written by the script.
- [ ] **Phase 6a — Exhaustive risk enumeration:** Identify ALL concrete risks visible in the
  affected flow from code evidence. Do not cap at three. For each risk record: Risk ID,
  Severity (CRITICAL/HIGH/MEDIUM/LOW), trigger + failure chain, user/system impact, evidence
  link, confidence (HIGH/MEDIUM/LOW), initial status (HYPOTHESIS/EVIDENCE-BACKED).
  Minimum risks to investigate for this change:
  - R-01: Duplicate order on double-click / retry (CRITICAL)
  - R-02: Concurrent same-key requests → unhandled `DataIntegrityViolationException` 500 (HIGH)
  - R-03: `payments.reference_id` has no UNIQUE constraint → duplicate payment rows (CRITICAL)
  - Scan for additional risks in: session expiry mid-checkout, missing `idempotencyKey` param
    (null key bypasses protection), cart-cleared-before-order-committed race condition.
- [ ] **Phase 6b — User selection gate:** Present the complete risk table to the requester.
  For each risk ask: Fix in this run? (YES / DEFER / WONT-FIX). Wait for explicit per-risk
  decisions before continuing. Do NOT select risks unilaterally.
- [ ] Record user decisions in change-brief.md risk table (YES / DEFER / WONT-FIX per risk ID).
- [ ] Write DEFERRED risks to `changesafe/business-rules.md` as open questions for future runs.
- [ ] **Phase 6c — Safety contract:** From YES risks only, select the highest-value risk for
  the safety contract; document selection rationale.
- [ ] **Present to human for Phase 8 approval** (change-brief + full risk table with decisions
  + selected safety contract + proposed tests + proposed code changes).
  Do not proceed to ST-3 until approved.
- [ ] Update change-brief.md status to APPROVED after human approval.

**Relevant Context:**
- [`changesafe/scripts/run-targeted-tests.ps1`](changesafe/scripts/run-targeted-tests.ps1)
- [`changesafe/templates/change-brief.md`](changesafe/templates/change-brief.md)

**Status:** [ ] pending

---

## ST-3 — Implementation: Database schema

**⛔ GATE: ST-2 must be APPROVED before this sub-task begins.**

**Intent:** Give the database a hard uniqueness constraint on the idempotency key.

**Expected Outcomes:**
- `schema.sql` has `idempotency_key VARCHAR(64) UNIQUE` (nullable) on the `orders` table.
- Existing positional INSERTs in application code and test fixtures switched to named-column form.

**Todo List:**
- [ ] Add `idempotency_key VARCHAR(64) UNIQUE` to `orders` CREATE TABLE in `schema.sql`.
- [ ] Switch `OrderJdbc` positional INSERT to named-column `INSERT INTO orders (id, total) VALUES (?, ?)`.
- [ ] Switch any test fixture SQL that uses positional INSERT on `orders` to named-column form.

**Relevant Context:**
- [`src/main/resources/schema.sql`](src/main/resources/schema.sql)
- [`src/main/java/com/ttulka/ecommerce/sales/order/jdbc/OrderJdbc.java`](src/main/java/com/ttulka/ecommerce/sales/order/jdbc/OrderJdbc.java)

**Status:** [x] done

---

## ST-4 — Implementation: Domain layer

**⛔ GATE: ST-2 must be APPROVED before this sub-task begins.**

**Intent:** Thread the idempotency key through the domain use-case and surface a typed exception.

**Expected Outcomes:**
- `PlaceOrder.DuplicateOrderException` nested class added.
- `PlaceOrder.place()` has `idempotencyKey` (nullable `String`) as 4th parameter.
- `PlaceOrderJdbc` pre-checks the key before INSERT; throws `DuplicateOrderException` if found.
- `OrderJdbc` writes the key to the `idempotency_key` column (null-safe).
- Existing `OrderAlreadyPlacedException` in-memory guard preserved.

**Todo List:**
- [ ] Add `DuplicateOrderException extends RuntimeException` nested class to `PlaceOrder`.
- [ ] Add `idempotencyKey` parameter to `PlaceOrder.place()`.
- [ ] Update `PlaceOrderJdbc.place()`: pre-check query; throw `DuplicateOrderException` if found.
- [ ] Update `OrderJdbc`: add `idempotencyKey` field; write to named-column INSERT.
- [ ] Update all callers that now need the new parameter (`PlaceOrderFromCart`, `FindOrdersJdbc`,
  test classes) — pass `null` as placeholder where not yet wired.

**Relevant Context:**
- [`src/main/java/com/ttulka/ecommerce/sales/order/PlaceOrder.java`](src/main/java/com/ttulka/ecommerce/sales/order/PlaceOrder.java)
- [`src/main/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderJdbc.java`](src/main/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderJdbc.java)
- [`src/main/java/com/ttulka/ecommerce/sales/order/jdbc/OrderJdbc.java`](src/main/java/com/ttulka/ecommerce/sales/order/jdbc/OrderJdbc.java)

**Status:** [x] done

---

## ST-5 — Implementation: Portal layer

**⛔ GATE: ST-2 must be APPROVED before this sub-task begins.**

**Intent:** Thread the idempotency key from controller to domain without bypassing clean-module
boundaries.

**Expected Outcomes:**
- `PlaceOrderFromCart.placeOrder()` has `idempotencyKey` parameter; replaces `null` placeholder.
- `CheckoutOrder.checkout()` both overloads have `idempotencyKey` parameter.
- `DuplicateOrderException` propagates; not caught at portal layer.

**Todo List:**
- [ ] Add `idempotencyKey` parameter to `PlaceOrderFromCart.placeOrder()`; replace `null`
  placeholder with actual value in call to `placeOrder.place()`.
- [ ] Add `idempotencyKey` parameter to both `CheckoutOrder.checkout()` overloads.
- [ ] Update `OrderController` call sites to pass `null` placeholder (wired in ST-6).
- [ ] Update `CheckoutOrderTest` and `PlaceOrderFromCartTest` call sites.

**Relevant Context:**
- [`src/main/java/com/ttulka/ecommerce/portal/PlaceOrderFromCart.java`](src/main/java/com/ttulka/ecommerce/portal/PlaceOrderFromCart.java)
- [`src/main/java/com/ttulka/ecommerce/portal/CheckoutOrder.java`](src/main/java/com/ttulka/ecommerce/portal/CheckoutOrder.java)

**Status:** [x] done

---

## ST-6 — Implementation: Web layer

**⛔ GATE: ST-2 must be APPROVED before this sub-task begins.**

**Intent:** Generate and persist the idempotency key in the HTTP session; wire it through the
form and controller; handle the duplicate case gracefully.

**Expected Outcomes:**
- GET handler reads `checkoutToken` from `HttpSession`; generates and stores UUID if absent.
- Thymeleaf form has `<input type="hidden" name="idempotencyKey" th:value="${checkoutToken}">`.
- POST handler reads `idempotencyKey`, passes to `checkoutOrder.checkout()`.
- On success: `session.removeAttribute("checkoutToken")` called before redirect.
- On `DuplicateOrderException`: redirects to `/order/error?message=duplicate` (no 500).
- Duplicate message key added to `messages.properties`.

**Todo List:**
- [ ] Update GET handler: read/generate/store `checkoutToken` in `HttpSession`; add to model.
- [ ] Add hidden `idempotencyKey` input to Thymeleaf checkout form template.
- [ ] Add `@RequestParam(required = false) String idempotencyKey` to POST handler.
- [ ] Pass `idempotencyKey` to both `checkoutOrder.checkout(...)` call sites.
- [ ] Add `session.removeAttribute("checkoutToken")` on success path.
- [ ] Add `catch (PlaceOrder.DuplicateOrderException e)` block with redirect.
- [ ] Add `order.error.msg.duplicate` key to `messages.properties`.

**Relevant Context:**
- [`src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java`](src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java)
- [`src/main/resources/templates/`](src/main/resources/templates/)
- [`src/main/resources/messages.properties`](src/main/resources/messages.properties)

**Status:** [x] done

---

## ST-7 — ChangeSafe Phase 9: Tests + after-fix test gate

**Intent:** Add safety tests for the duplicate path and capture the after-fix baseline using
the canonical script, producing `after-fix-test-summary.json` in the evidence directory.

**⚠️ CRITICAL: Use `run-targeted-tests.ps1 -Phase after-fix`, NOT bare Maven.
The script writes the JSON summary file that the validator requires. Bare Maven does not.**

**Expected Outcomes:**
- New `@JdbcTest` test `duplicate_idempotency_key_throws_exception()` in `PlaceOrderTest`.
- New `@SpringBootTest` test `duplicate_order_submission_does_not_create_a_second_order()` in
  `OrderWorkFlowTest`.
- `changesafe/evidence/checkout-idempotency-01/after-fix-test-summary.json` written by script.
- Full suite (307+ tests) passes with 0 failures.

**Todo List:**
- [ ] Add `duplicate_idempotency_key_throws_exception()` to `PlaceOrderTest`: call `place()` twice
  with same key; assert `DuplicateOrderException`; assert `eventPublisher.raise` called once;
  assert DB count = 1.
- [ ] Add `duplicate_order_submission_does_not_create_a_second_order()` to `OrderWorkFlowTest`:
  POST `/order` twice with same `idempotencyKey`; assert HTTP 302 on second call; assert
  `orders` count = 1.
- [ ] Run after-fix gate **using the script**:
  ```
  changesafe/scripts/run-targeted-tests.ps1 -RunId checkout-idempotency-01 \
      -Tests "PlaceOrderTest,OrderTest,OrderWorkFlowTest,OrderControllerTest" -Phase after-fix
  ```
- [ ] Confirm `after-fix-test-summary.json` written to evidence directory.
- [ ] Run full suite to confirm 0 regressions:
  ```
  changesafe/scripts/run-targeted-tests.ps1 -RunId checkout-idempotency-01 \
      -Tests "ALL" -Phase after-fix
  ```

**Relevant Context:**
- [`src/test/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderTest.java`](src/test/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderTest.java)
- [`src/test/java/com/ttulka/ecommerce/OrderWorkFlowTest.java`](src/test/java/com/ttulka/ecommerce/OrderWorkFlowTest.java)
- [`changesafe/scripts/run-targeted-tests.ps1`](changesafe/scripts/run-targeted-tests.ps1)

**Status:** [ ] pending

---

## ST-8 — ChangeSafe Phase 10: Evaluate acceptance criteria

**Intent:** Formally evaluate each AC as MET / NOT MET / NOT VERIFIED using actual test
evidence — not assumed from test success alone.

**Expected Outcomes:**
- Each of AC-01 through AC-04 assigned a status with a specific evidence reference.
- Overall goal status determined by the deterministic rule (all MET → MET; any NOT MET → NOT MET;
  any NOT VERIFIED → NOT VERIFIED).

**Todo List:**
- [ ] Read `after-fix-test-summary.json`; record `testsRun`, `failures`, `errors`.
- [ ] Evaluate AC-01 (DuplicateOrderException + single DB row) → evidence: PlaceOrderTest.
- [ ] Evaluate AC-02 (OrderPlaced raised once) → evidence: verify(eventPublisher, times(1)).
- [ ] Evaluate AC-03 (HTTP 302, no second row) → evidence: OrderWorkFlowTest integration test.
- [ ] Evaluate AC-04 (session token cleared on success) → evidence: OrderControllerTest.
- [ ] Apply deterministic goal rule; record overall status.

**Status:** [ ] pending

---

## ST-9 — ChangeSafe Phase 11: Deliver evidence pack + validate

**⚠️ CRITICAL: This sub-task is MANDATORY. The run is NOT complete until the validator exits 0.**

**Intent:** Fill the two remaining evidence files, run the structural validator, and fix any
failures it reports.

**Expected Outcomes:**
- `changesafe/evidence/checkout-idempotency-01/risk-report.md` written with all 8 required
  sections, no `<placeholder>` text, controlled vocabulary throughout.
- `changesafe/evidence/checkout-idempotency-01/comparison.md` written with all 5 required
  sections.
- `changesafe/scripts/validate-output.ps1 -RunDir "changesafe/evidence/checkout-idempotency-01"`
  exits with code 0 and reports 0 failures.
- Team reminded to capture Bob session consumption screenshot and update `bob_sessions/INDEX.md`.

**Todo List:**
- [ ] Copy `changesafe/templates/risk-report.md` to
  `changesafe/evidence/checkout-idempotency-01/risk-report.md`.
- [ ] Fill all 8 sections of risk-report.md with actual evidence (no `<placeholder>` text):
  - Section 1: goal, ACs (AC-01–AC-04), scope.
  - Section 2: cheap test gate results from both JSON summaries.
  - Section 3: impact map (all components from ST-2 impact analysis).
  - Section 4: ALL risks from the Phase 6a enumeration — each with Severity, Fix decision
    (YES/DEFER/WONT-FIX), and final Status (PREVENTED/UNRESOLVED/DEFERRED/WONT-FIX etc.).
    Summary lines: "Risks fixed in this run", "Risks deferred", "Risks WONT-FIX".
  - Section 5: safety contract for the selected YES risk.
  - Section 6: implementation verification table; approved changes list; conventions.
  - Section 7: AC goal check table; overall goal status; remaining in-scope risk;
    **Deferred risk backlog table** — one row per DEFERRED risk with suggested follow-on run ID.
  - Section 8: evidence provenance (before/after JSON paths, comparison.md, change-brief.md).
- [ ] Copy `changesafe/templates/comparison.md` to
  `changesafe/evidence/checkout-idempotency-01/comparison.md`.
- [ ] Fill all 5 sections of comparison.md.
- [ ] Run the validator:
  ```
  changesafe/scripts/validate-output.ps1 -RunDir "changesafe/evidence/checkout-idempotency-01"
  ```
- [ ] If validator exits non-zero: fix every reported failure before marking ST-9 done.
- [ ] Remind team: capture Bob consumption screenshot → `bob_sessions/NN-checkout-idempotency-consumption.png`;
  update `bob_sessions/INDEX.md`.

**Relevant Context:**
- [`changesafe/templates/risk-report.md`](changesafe/templates/risk-report.md)
- [`changesafe/templates/comparison.md`](changesafe/templates/comparison.md)
- [`changesafe/scripts/validate-output.ps1`](changesafe/scripts/validate-output.ps1)

**Status:** [ ] pending

---

## ST-10 — ChangeSafe Phase 12: Commit, push, PR (only with explicit authorization)

**⛔ GATE: Requires explicit PR authorization from the requester. This is NOT the default outcome.**

**Intent:** If and only if the requester explicitly authorizes a PR, commit, push, and create
the pull request using the filled PR description template.

**Todo List:**
- [ ] Confirm exact authorization scope and reference.
- [ ] Copy `changesafe/templates/pr-description.md` to
  `changesafe/evidence/checkout-idempotency-01/pr-description.md`; fill it.
- [ ] Commit all changes; push branch; create PR.

**Status:** [ ] pending

---

## Confirmed Business Rules

| Rule ID | Rule | Source |
|---------|------|--------|
| BR-001 | The same checkout submission must never create more than one order or raise more than one OrderPlaced event | Requester confirmation — this session |
| BR-002 | `idempotency_key` column must be nullable to preserve backward compatibility | Q-01 confirmed by requester |
| BR-003 | On duplicate submission the user sees a clear "already submitted" message, not a 500 | Q-02 confirmed by requester |
| BR-004 | Idempotency key is stored in HTTP session; generated once on GET, cleared on successful POST | Q-03 confirmed by requester |

## Confirmed Acceptance Criteria

| ID | Criterion | Verification method |
|----|-----------|---------------------|
| AC-01 | Second call to `PlaceOrder.place()` with same key throws `DuplicateOrderException`; exactly one `orders` row in DB | `PlaceOrderTest.duplicate_idempotency_key_throws_exception()` |
| AC-02 | `OrderPlaced` event raised exactly once per idempotency key | `verify(eventPublisher, times(1)).raise(any(OrderPlaced.class))` |
| AC-03 | Second HTTP POST with same key returns HTTP 302; no second `orders` row | `OrderWorkFlowTest.duplicate_order_submission_does_not_create_a_second_order()` |
| AC-04 | Session `checkoutToken` cleared on successful checkout | `OrderControllerTest` verifies `session.removeAttribute("checkoutToken")` |

## Conventions applied

- No `Impl`, `DTO`, `Service`, `Repository` class name suffixes (enforced by `CleanCodeArchTest`).
- JDBC implementation naming: `PlaceOrderJdbc` (existing class, extended in place).
- `@Transactional` on `PlaceOrderJdbc.place()` (already present; retained).
- `@NonNull` on all new constructor and method parameters.
- No cross-domain direct calls; event flow unchanged.
- New exception follows the nested-class pattern: `PlaceOrder.DuplicateOrderException`.
- Snake_case test method names; `assertAll` + Mockito `argThat`; `@MockBean EventPublisher`.
