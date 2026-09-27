# ChangeSafe Risk Report — Checkout Flow Audit

| Field | Value |
| --- | --- |
| Run ID | checkout-flow-01 |
| Generated at | 2026-09-26T20:40:16+08:00 |
| Requirement / change | Broad checkout flow audit: identify all latent issues and enhancements (user request, chat) |
| Before-fix source state | Commit 274af07a38bf541d8a434a34b375ffe1479b17b7 with historical uncommitted changes; exact historical diff NOT VERIFIED |
| After-fix source state | Commit 274af07a38bf541d8a434a34b375ffe1479b17b7 plus current working-tree changes; no commit created |
| Bob task ID / title | 9b5771306d8cd2fdacf3c5b6880e5e99 — checkout flow audit; interrupted, subsequently reviewed by Codex |
| Project rule | [.bob/rules/01-project-conventions.md](../../../.bob/rules/01-project-conventions.md) — DRAFT · awaiting review |
| Change brief | [change-brief.md](change-brief.md) |
| Business decisions | BR-001 through BR-005; [changesafe/business-rules.md](../../business-rules.md) |
| Report status | NEEDS REVIEW |

## 1. Goal and scope

**Goal:** Implement the nine approved checkout risk mitigations and verify the eight agreed acceptance criteria within the available evidence.

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | Submitting the same idempotency key twice creates exactly one order row and returns 302 | HTTP same-token replay not demonstrated: workflow test obtains a fresh token and only asserts redirect. |
| AC-02 | After DuplicateOrderException: cart is cleared, session token is refreshed, user sees duplicate message | Cart clearing is asserted; session-token refresh and rendered duplicate message are not fully asserted. |
| AC-03 | Concurrent double-submit with same key: second thread gets graceful 302/error, not 500 | Existing domain test is sequential; concurrent HTTP/transaction behavior is untested. |
| AC-04 | Checkout blocked when any cart item is out of stock | Mocked insufficient-stock rejection and controller error routing pass; this does not verify concurrent stock safety. |
| AC-05 | CART_ID cookie has HttpOnly, Secure (in prod), SameSite=Strict flags | Unit tests cover HttpOnly, SameSite and HTTPS Secure=true; production HTTPS and HTTP Secure=false are not fully verified. |
| AC-06 | CartJdbc.items() always reads from DB (no stale in-process cache) | Cache removal observed in code; a live cart mutation regression was not demonstrated by the selected baseline. |
| AC-07 | order-error.html only renders known message codes; unknown code falls back to default | Unknown message-code fallback is asserted by the passing controller test. |
| AC-08 | Client-supplied cart price is re-validated against catalog price at checkout | Catalog-price calculation is asserted by a passing JDBC-slice test; no pre-fix replay exists. |

**In scope:** `PlaceOrderJdbc`, `OrderController`, `CheckoutOrder`, `PlaceOrderFromCart`, `CartIdFromCookies`, `CartJdbc`, `OrderWorkFlowTest`, `order-error.html`, `messages.properties`, `PortalConfig`

**Out of scope:** R-07 (async listener error handling — infrastructure decision deferred); catalog browsing; login; payment gateway; admin.

**Unresolved business questions:** Q-05 resolved (Option A — portal layer). Q-06 deferred with R-07.

## 2. Cheap test gate

| Selected tests | Command | Result | Exit code | Duration (s) | Summary / raw log |
| --- | --- | --- | --- | --- | --- |
| PlaceOrderTest, OrderControllerTest, CheckoutOrderTest, PlaceOrderFromCartTest, OrderWorkFlowTest | Exact command in linked summary JSON | PASS | 0 | 34.9 | [before-fix-test-summary.json](logs/../before-fix-test-summary.json) |
| PlaceOrderTest, OrderControllerTest, CheckoutOrderTest, PlaceOrderFromCartTest, OrderWorkFlowTest, CartIdFromCookiesTest | Exact command in linked summary JSON | PASS | 0 | 42.4 | [after-fix-test-summary.json](after-fix-test-summary.json) |

**Reason to stop or continue:** Both gates pass; 8 additional tests added; zero regressions. Continue to evidence pack.

## 3. Change-impact map

| Component | Direct / downstream | Observed path or dependency | Evidence |
| --- | --- | --- | --- |
| `PlaceOrderJdbc` | Direct | Catches `DataIntegrityViolationException` → re-throws `DuplicateOrderException` | [PlaceOrderJdbc.java](../../../src/main/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderJdbc.java) |
| `OrderController` | Direct | Reads idempotency key from session; clears cart + refreshes token on duplicate; whitelist for error codes | [OrderController.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java) |
| `CheckoutOrder` | Direct | Added `Warehouse` dependency; `checkStock()` pre-flight; `OutOfStockException` | [CheckoutOrder.java](../../../src/main/java/com/ttulka/ecommerce/portal/CheckoutOrder.java) |
| `PlaceOrderFromCart` | Direct | Added `FindProducts` dependency; total re-priced from catalog | [PlaceOrderFromCart.java](../../../src/main/java/com/ttulka/ecommerce/portal/PlaceOrderFromCart.java) |
| `PortalConfig` | Direct (wiring) | Adds `FindProducts` to `PlaceOrderFromCart` bean; `Warehouse` to `CheckoutOrder` bean | [PortalConfig.java](../../../src/main/java/com/ttulka/ecommerce/portal/PortalConfig.java) |
| `CartIdFromCookies` | Direct | Cookie flags: `HttpOnly=true`, `Secure=request.isSecure()`, `SameSite=Strict` | [CartIdFromCookies.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/CartIdFromCookies.java) |
| `CartJdbc` | Direct | Removed in-memory `items` cache; always queries DB | [CartJdbc.java](../../../src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java) |
| `messages.properties` | Downstream (i18n) | Added `order.error.msg.outofstock` key | [messages.properties](../../../src/main/resources/messages.properties) |
| `order-error.html` | No change | Already renders `messageCode`; now only receives whitelisted values | [order-error.html](../../../src/main/resources/templates/order-error.html) |

## 4. Future incidents

| Risk ID | Severity | Trigger and incident | User / system impact | Evidence | Confidence | Fix decision | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R-01 | HIGH | Session token not cleared after DuplicateOrderException → retry loop | User stuck with stale token; cannot retry cleanly | [OrderController.java:93](../../../src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java) | HIGH | YES | EVIDENCE-BACKED |
| R-02 | CRITICAL | Client-supplied idempotencyKey never validated against server session token | Duplicate protection bypassable by client | [OrderController.java:60](../../../src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java) | HIGH | YES | EVIDENCE-BACKED |
| R-03 | MEDIUM | Cart not cleared after duplicate rejection — order placed but cart still full | User confused; re-submit loops on duplicate error | [CheckoutOrder.java:35](../../../src/main/java/com/ttulka/ecommerce/portal/CheckoutOrder.java) | HIGH | YES | EVIDENCE-BACKED |
| R-04 | CRITICAL | SELECT-then-INSERT race on idempotency key → `DataIntegrityViolationException` → unhandled 500 | Concurrent submit causes 500 instead of graceful duplicate error | [PlaceOrderJdbc.java:40](../../../src/main/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderJdbc.java) | MEDIUM | YES | EVIDENCE-BACKED |
| R-05 | HIGH | No pre-flight stock check → out-of-stock orders placed silently | Order confirmed; `GoodsMissed` raised with no consumer; goods never fully dispatched | [GoodsFetchingJdbc.java:40](../../../src/main/java/com/ttulka/ecommerce/warehouse/jdbc/GoodsFetchingJdbc.java) | HIGH | YES | EVIDENCE-BACKED |
| R-06 | HIGH | `CART_ID` cookie missing HttpOnly/Secure/SameSite | XSS/session sharing on shared or HTTP machines | [CartIdFromCookies.java:55](../../../src/main/java/com/ttulka/ecommerce/portal/web/CartIdFromCookies.java) | HIGH | YES | EVIDENCE-BACKED |
| R-07 | HIGH | Async listeners swallow errors → silent payment/warehouse failure | Order placed, user sees success, no payment or goods ever dispatched | [billing/OrderPlacedListener.java](../../../src/main/java/com/ttulka/ecommerce/billing/payment/listeners/OrderPlacedListener.java) | HIGH | DEFER | DEFERRED |
| R-08 | MEDIUM | `CartJdbc.items()` in-memory cache → stale data on concurrent mutation | Order placed with incorrect quantities/prices | [CartJdbc.java:32](../../../src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java) | MEDIUM | YES | EVIDENCE-BACKED |
| R-09 | CRITICAL | Cart price is client-supplied; never re-validated against catalog | Revenue loss via price manipulation | [PlaceOrderFromCart.java:43](../../../src/main/java/com/ttulka/ecommerce/portal/PlaceOrderFromCart.java) | HIGH | YES | EVIDENCE-BACKED |
| R-10 | MEDIUM | `order-error.html` uses user-controlled `messageCode` in i18n key | Rendering exception or undefined message for injected codes | [order-error.html](../../../src/main/resources/templates/order-error.html) | MEDIUM | YES | EVIDENCE-BACKED |

**Risks fixed in this run:** Implementation changes applied for R-01, R-02, R-03, R-04, R-05, R-06, R-08, R-09, R-10; complete mitigation is NOT VERIFIED.

**Risks deferred to backlog:** R-07

**Risks marked WONT-FIX:** N/A — none

**Selection rationale:** R-09 selected as the primary safety contract because it is CRITICAL severity with direct, irreversible financial impact (any user can pay an arbitrary price). R-04 is also CRITICAL but is defended by the existing DB UNIQUE constraint which prevents data corruption — only the user experience is broken. R-09 has no DB-level defence.

## 5. Selected safety contract

| Field | Value |
| --- | --- |
| Linked risk | R-09 |
| Safety invariant | The total charged at checkout equals `sum(catalogPrice × quantity)` for every cart item — the client-supplied cart price is never used for billing |
| Adverse condition | Cart item with a manipulated `price` field (e.g., 99.99 instead of catalog 1.00) |
| Reproduction / regression test | [`PlaceOrderFromCartTest.checkout_uses_catalog_price_not_cart_price`](../../../src/test/java/com/ttulka/ecommerce/portal/PlaceOrderFromCartTest.java) |
| Expected result | `OrderPlaced.total` = catalogPrice × quantity (not cart price × quantity) |
| Actual before-fix result | NOT RUN — new test was not run against the pre-fix implementation; code evidence only |
| Actual after-fix result | PASS — `OrderJdbc` logs `Money(2.0)` for product "1" at qty 2 with catalog price 1.00, regardless of the 99.99 cart price. Confirmed by after-fix log. |

## 6. Implementation and verification

| Item | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test (R-09) | Not present | `PlaceOrderFromCartTest.checkout_uses_catalog_price_not_cart_price` PASS | [after-fix-test-summary.json](after-fix-test-summary.json) |
| Targeted tests (all classes) | 24 run / 0 failures | 32 run / 0 failures (+8 new tests) | Before / after summary JSON |
| Full suite (Codex continuation) | NOT VERIFIED — no retained full-suite baseline | PASS — 315 tests, 0 failures/errors/skips | [Copied Surefire XML](codex-handoff/surefire), executed by Codex, not Bob |
| `PlaceOrderJdbc` — R-04 | `DataIntegrityViolationException` not caught → 500 | Caught; re-thrown as `DuplicateOrderException` | [PlaceOrderJdbc.java](../../../src/main/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderJdbc.java) |
| `OrderController` — R-01/R-02/R-03 | Client key used; token not refreshed; cart not cleared on duplicate | Session token used; token refreshed on duplicate; cart cleared | [OrderController.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java) |
| `CheckoutOrder` — R-05 | No stock pre-flight | `checkStock()` called before `placeOrderFromCart.placeOrder()`; `OutOfStockException` on insufficient stock | [CheckoutOrder.java](../../../src/main/java/com/ttulka/ecommerce/portal/CheckoutOrder.java) |
| `CartIdFromCookies` — R-06 | No security flags | `HttpOnly=true`, `Secure=request.isSecure()`, `SameSite=Strict` | [CartIdFromCookies.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/CartIdFromCookies.java) |
| `CartJdbc` — R-08 | Cached `items` field; stale on mutation | Field removed; always queries DB | [CartJdbc.java](../../../src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java) |
| `PlaceOrderFromCart` — R-09 | Cart price used for total | Catalog price via `findProducts.byId()` used for total | [PlaceOrderFromCart.java](../../../src/main/java/com/ttulka/ecommerce/portal/PlaceOrderFromCart.java) |
| `OrderController.error()` — R-10 | Any `message` param passed to template | Only KNOWN_ERROR_CODES accepted; others → "default" | [OrderController.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java) |

**Approved code changes:** 7 production files modified (`PlaceOrderJdbc`, `OrderController`, `CheckoutOrder`, `PlaceOrderFromCart`, `PortalConfig`, `CartIdFromCookies`, `CartJdbc`) + `messages.properties`; 4 test files modified/created (`CheckoutOrderTest`, `OrderControllerTest`, `PlaceOrderFromCartTest`, `CartIdFromCookiesTest`) + `OrderWorkFlowTest` updated.

**Implementation approval:** User (chat), run checkout-flow-01 — APPROVED (all YES risks + Option A for R-09)

**Project conventions followed:**
- `@RequiredArgsConstructor` + `@NonNull` for constructor injection (`CheckoutOrder`, `PlaceOrderFromCart`)
- `@Transactional` on `CheckoutOrder.checkout()` methods
- No banned class-name suffixes (verified)
- snake_case test method names throughout
- JDBC implementation stays package-private in `jdbc/` sub-package
- `CleanModulesArchTest` compliance: `portal` depends on `warehouse` and `sales.catalog` use-case interfaces only — not their `jdbc/` implementations

**Approved deviations:** None.

**Review findings:** Codex continuation identified missing same-token replay/concurrency coverage, non-atomic stock preflight, incomplete token assertions, overly broad integrity-error translation, and report overclaims. Bob /review was not run; absence of a PR is not a reason to skip review.

## 7. Goal check and remaining risk

| ID | Criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | Submitting the same idempotency key twice creates exactly one order row and returns 302 | NOT VERIFIED | HTTP same-token replay not demonstrated: workflow test obtains a fresh token and only asserts redirect. |
| AC-02 | After DuplicateOrderException: cart is cleared, session token is refreshed, user sees duplicate message | NOT VERIFIED | Cart clearing is asserted; session-token refresh and rendered duplicate message are not fully asserted. |
| AC-03 | Concurrent double-submit with same key: second thread gets graceful 302/error, not 500 | NOT VERIFIED | Existing domain test is sequential; concurrent HTTP/transaction behavior is untested. |
| AC-04 | Checkout blocked when any cart item is out of stock | MET | Mocked insufficient-stock rejection and controller error routing pass; this does not verify concurrent stock safety. |
| AC-05 | CART_ID cookie has HttpOnly, Secure (in prod), SameSite=Strict flags | NOT VERIFIED | Unit tests cover HttpOnly, SameSite and HTTPS Secure=true; production HTTPS and HTTP Secure=false are not fully verified. |
| AC-06 | CartJdbc.items() always reads from DB (no stale in-process cache) | NOT VERIFIED | Cache removal observed in code; a live cart mutation regression was not demonstrated by the selected baseline. |
| AC-07 | order-error.html only renders known message codes; unknown code falls back to default | MET | Unknown message-code fallback is asserted by the passing controller test. |
| AC-08 | Client-supplied cart price is re-validated against catalog price at checkout | MET | Catalog-price calculation is asserted by a passing JDBC-slice test; no pre-fix replay exists. |

**Overall goal:** NOT VERIFIED — five of eight criteria lack complete evidence; passing selected tests do not establish their full coverage.

**Remaining risk (in-scope, not fixed):** Same-token HTTP replay and concurrent checkout are not demonstrated. Stock preflight does not atomically reserve inventory. All DataIntegrityViolationException instances are translated to duplicates. Missing tokens remain accepted. R-07 remains deferred. Project conventions remain DRAFT; session evidence completeness needs human review.

**Bob advisory recommendation:** Historical proceed recommendation withdrawn during Codex evidence review; investigate remaining gaps before release. No release approval is granted.

### Deferred risk backlog

| Risk ID | Severity | Summary | Reason deferred | Suggested follow-on run ID |
| --- | --- | --- | --- | --- |
| R-07 | HIGH | Async `@TransactionalEventListener @Async` listeners swallow exceptions → silent order stall | Infrastructure decision required (retry policy, dead-letter, alerting); outside current run scope | checkout-async-resilience-01 |

**Human go/no-go decision:** ____________________
**Reviewer and date:** ____________________

## 8. Evidence provenance

| Artifact | Path / task reference | Captured at |
| --- | --- | --- |
| Before-fix test log | [logs/before-fix-20260926-200012.log](logs/before-fix-20260926-200012.log) | 2026-09-26T20:00:12+08:00 |
| After-fix test log | [logs/after-fix-20260926-202817.log](logs/after-fix-20260926-202817.log) | 2026-09-26T20:28:17+08:00 |
| Before/after comparison | [comparison.md](comparison.md) | 2026-09-26T20:29:00+08:00 |
| Change brief | [change-brief.md](change-brief.md) | 2026-09-26 |
| Evaluation hints | NOT VERIFIED — no explicit defect-location hints identified, but requester supplied business and design decisions; independent discovery must not be overstated | N/A |
| Bob consumption screenshot | NOT VERIFIED — not yet captured; save to `bob_sessions/NN-checkout-flow-audit-consumption.png` | N/A |


**Continuation provenance:** Codex completed evidence review and documentation after Bobcoin exhaustion. Original report and original JSON counts are retained under [codex-handoff](codex-handoff). No application changes were made during continuation.
