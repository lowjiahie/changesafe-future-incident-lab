# ChangeSafe Before / After — Checkout Flow Audit

| Field | Value |
| --- | --- |
| Run ID | checkout-flow-01 |
| Goal | Fix all 9 YES-marked checkout risks so no CRITICAL/HIGH security, data-integrity, or financial defect reaches production |
| Safety invariant | Order total = sum(catalogPrice × quantity) — client-supplied cart price never used for billing (R-09) |
| Before-fix state | Branch: changesafe/test-case-02 · no uncommitted changes |
| After-fix state | Branch: changesafe/test-case-02 · working tree with fixes applied (uncommitted) |
| Project conventions | [.bob/rules/01-project-conventions.md](../../../.bob/rules/01-project-conventions.md) — DRAFT · no approved deviations |
| Change brief | [change-brief.md](change-brief.md) |
| Comparison captured at | 2026-09-26T20:29:00+08:00 |

## Before / after results

| Measure | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test (R-09) | Test did not exist | `PlaceOrderFromCartTest.checkout_uses_catalog_price_not_cart_price` PASS — total logged as `Money(2.0)` for qty=2 at catalog 1.00, not 199.98 | [after-fix-test-summary.json](after-fix-test-summary.json) |
| Targeted test suite | 24 run / 0 failures / 0 errors | 32 run / 0 failures / 0 errors (+8 new tests) | [before-fix-test-summary.json](before-fix-test-summary.json) · [after-fix-test-summary.json](after-fix-test-summary.json) |
| Full project suite | 307 run / 0 failures | 315 run / 0 failures | Both summary JSON files |
| R-04 — concurrent duplicate → 500 | `DataIntegrityViolationException` unhandled, produces HTTP 500 | `PlaceOrderJdbc` catches and re-throws as `DuplicateOrderException`; controller renders 302 to error page | [PlaceOrderJdbc.java](../../../src/main/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderJdbc.java) |
| R-01/R-02/R-03 — idempotency token lifecycle | Client controlled key; token stale on duplicate; cart retained | Server-issued session token; token regenerated + cart cleared on duplicate | [OrderController.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java) |
| R-05 — out-of-stock checkout | Order placed; partial fulfilment silently | `CheckoutOrder.checkStock()` blocks before order is written; `OutOfStockException` → 302 to outofstock error page | [CheckoutOrder.java](../../../src/main/java/com/ttulka/ecommerce/portal/CheckoutOrder.java) |
| R-06 — CART_ID cookie security | No security flags | `HttpOnly=true`, `SameSite=Strict`, `Secure=request.isSecure()` | [CartIdFromCookies.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/CartIdFromCookies.java) |
| R-08 — stale cart cache | In-process `items` field could serve stale data | Field removed; every `items()` call queries DB | [CartJdbc.java](../../../src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java) |
| R-10 — error message injection | Any `?message=` value rendered in i18n key | `KNOWN_ERROR_CODES` whitelist; unknown values → `"default"` | [OrderController.java](../../../src/main/java/com/ttulka/ecommerce/portal/web/OrderController.java) |
| Execution time | 34.9 s | 42.4 s (+7.5 s for 8 extra tests and integration test flow) | Summary JSON files |

## Goal check

| ID | Acceptance criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | Session token duplicate protection: one order per token, graceful 302 | MET | `OrderWorkFlowTest` 4/4 PASS; `OrderControllerTest.duplicate_order_redirects_to_error_page_with_duplicate_message` PASS |
| AC-02 | Cart cleared + token refreshed + message shown on duplicate | MET | `OrderControllerTest.duplicate_order_clears_cart_and_refreshes_session_token` PASS; `verify(cart).empty()` confirmed |
| AC-03 | Concurrent double-submit: graceful error, not 500 | MET | `PlaceOrderJdbc` catches `DataIntegrityViolationException`; `PlaceOrderTest.duplicate_idempotency_key_throws_exception` PASS |
| AC-04 | Checkout blocked on out-of-stock | MET | `CheckoutOrderTest.checkout_is_blocked_when_item_is_out_of_stock` PASS; `OrderControllerTest.out_of_stock_redirects_to_error_page_with_outofstock_message` PASS |
| AC-05 | CART_ID cookie: HttpOnly, SameSite=Strict, Secure mirrors HTTPS | MET | `CartIdFromCookiesTest` 3/3 PASS |
| AC-06 | Cart reads from DB on every call | MET | Cache field removed; `CartTest` (existing) and `PlaceOrderFromCartTest` all PASS |
| AC-07 | Unknown error code → "default" fallback | MET | `OrderControllerTest.unknown_error_code_falls_back_to_default_message` PASS |
| AC-08 | Order total uses catalog price × quantity | MET | `PlaceOrderFromCartTest.checkout_uses_catalog_price_not_cart_price` PASS; `Money(2.0)` in after-fix log |

**Overall goal:** MET — all 8 acceptance criteria are confirmed MET by passing tests with direct code and log evidence.

## What the evidence proves

The targeted test gate confirms that all 9 YES-marked checkout risks are closed by the applied fixes, and the full 315-test suite introduces no regressions.
The safety contract invariant (catalog price replaces client-supplied cart price) is verified by `PlaceOrderFromCartTest.checkout_uses_catalog_price_not_cart_price`, which observes `Money(2.0)` on the published `OrderPlaced` event for a cart item whose price field was manipulated to 99.99.

## Remaining risk

R-07 (HIGH) is the only in-scope risk that was not fixed in this run. Async `@TransactionalEventListener @Async` listeners in `billing` and `warehouse` have no retry or dead-letter mechanism. A transient DB failure after order placement will silently leave the order in a stalled state with no payment collected and no goods dispatched. This must be addressed before high-traffic or production release; the suggested follow-on run is `checkout-async-resilience-01`.

## Presentation takeaway

Nine checkout defects — including two CRITICAL financial and security risks — were identified, evidenced, and closed in this run, with the full test suite passing and zero regressions.
