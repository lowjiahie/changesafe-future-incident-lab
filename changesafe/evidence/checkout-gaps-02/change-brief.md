# ChangeSafe Change Brief — Checkout Business Rules Gap Fixes

| Field | Value |
| --- | --- |
| Run ID | checkout-gaps-02 |
| Requester | User (chat) |
| Requirement source | User request: follow-on to checkout-flow-01 — deeper business-rules audit; gaps identified via flow trace and code analysis |
| Source state | Commit 80d91f80268b4a6ecf893c475677d4e6c0598d8a (HEAD at intake); all changes are working-tree modifications |
| Status | COMPLETE |

## 1. Problem and goal

**Problem:** The prior run (`checkout-flow-01`) fixed 10 risks (R-01 to R-10) but left the
`changesafe/business-rules.md` register empty and did not address a further set of gaps found
during a deeper audit of the full order flow: blank address accepted, silent wrong total when a
product is deleted mid-session, dead form field confusing the idempotency contract, no checkout
audit log, no order reference for guests, overselling via non-atomic stock check, and undocumented
CSRF dual-use of the checkout token.

**Goal:** Fix all in-scope gaps, populate the business-rules register, and leave 324 passing
tests as the evidence baseline.

## 2. Scope and exclusions

**In scope:**
- `Place.java` — blank address fix (BR-011)
- `PlaceOrderFromCart.java` — deleted-product detection and `ProductNotFoundException` (BR-012)
- `OrderController.java` — `productnotfound` handler, `KNOWN_ERROR_CODES`, audit log, `RedirectAttributes` for orderId, CSRF Javadoc
- `order.html` — remove dead `idempotencyKey` field; add `required` to address textarea
- `order-success.html` — guest order reference display (BR-010)
- `messages.properties` — `productnotfound` and `reference.label` i18n keys
- `Warehouse.java` — `leftInStockForUpdate()` default method (BR-013)
- `WarehouseJdbc.java` — `SELECT ... FOR UPDATE` override
- `CheckoutOrder.java` — `checkStock()` calls `leftInStockForUpdate()`
- `changesafe/business-rules.md` — populated: BR-001 to BR-013, Q-01, D-001 to D-008
- Test updates: `PlaceTest`, `PlaceOrderFromCartTest`, `CheckoutOrderTest`, `OrderAddressRequiredTest`

**Out of scope:** Async listener failure recovery (R-07, deferred to `checkout-async-resilience-01`),
email confirmation for guests, order lookup endpoint, maximum cart size, minimum order value (BR-008 confirmed $0 valid), concurrent-checkout integration test (H2 limitation).

## 3. Business rules and open questions

| Rule ID | Confirmed rule | Source / decision |
| --- | --- | --- |
| BR-007 | Checkout token is server-issued, session-bound; serves as both idempotency key and implicit CSRF mitigation | checkout-gaps-02 planning; user confirmed |
| BR-008 | $0 orders are valid; no minimum order value | checkout-gaps-02 planning; user confirmed |
| BR-009 | Guest checkout always permitted; no login required | checkout-gaps-02 planning; current behavior confirmed intentional |
| BR-010 | Guest orders show order reference number on success page; no email or lookup endpoint | checkout-gaps-02 planning; user confirmed |
| BR-011 | Delivery address must be non-blank after strip and ≤ 100 characters | checkout-gaps-02 planning; GAP-01 code fix |
| BR-012 | Cart item referencing deleted product → reject with user-facing error, not 500 | checkout-gaps-02 planning; GAP-03 code fix |
| BR-013 | Stock checked under pessimistic lock (SELECT … FOR UPDATE) at checkout | checkout-gaps-02 planning; user chose Option B (pessimistic lock) |

| Question ID | Question | Answer / status | Blocks |
| --- | --- | --- | --- |
| Q-01 | Async listener failure recovery strategy — retry, DLQ, alerting? | OPEN — deferred to checkout-async-resilience-01 | R-07 deferred |

## 4. Acceptance criteria

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | Whitespace-only delivery address is rejected before checkout proceeds | `PlaceTest` + `OrderAddressRequiredTest` parametric tests |
| AC-02 | Cart item referencing a deleted product causes `ProductNotFoundException`, routed to `productnotfound` error page | `PlaceOrderFromCartTest.checkout_fails_when_product_not_in_catalog` |
| AC-03 | Dead `idempotencyKey` hidden field removed from `order.html` | Code inspection; 324 tests pass |
| AC-04 | Checkout attempt logged with username/guest, IP, cart ID at INFO level | Code inspection; log line in `OrderController.place()` |
| AC-05 | `SELECT … FOR UPDATE` used in `WarehouseJdbc.leftInStockForUpdate()` and called by `CheckoutOrder.checkStock()` | Code inspection + `CheckoutOrderTest` |
| AC-06 | Order reference UUID shown on success page; absent without flash attribute (e.g. page reload) | Code inspection; Thymeleaf `th:if` guard |
| AC-07 | `changesafe/business-rules.md` has 13 confirmed rules (BR-001 to BR-013) | Document inspection |
| AC-08 | All 324 tests pass with no new warnings | `./mvnw clean test` — BUILD SUCCESS |

## 5. Implementation and verification plan

| Step | Planned action | Status |
| --- | --- | --- |
| 1 | Populate `business-rules.md`; add CSRF Javadoc | ✅ Done |
| 2 | Fix `Place.java` blank check; update `PlaceTest`; add `required` to textarea | ✅ Done |
| 3 | Remove dead `idempotencyKey` field from `order.html` | ✅ Done |
| 4 | Add `@Slf4j` + audit log to `OrderController` | ✅ Done |
| 5 | Add `ProductNotFoundException` in `PlaceOrderFromCart`; wire to controller and i18n | ✅ Done |
| 6 | Capture `orderId` in `OrderController`; flash to success page; update template | ✅ Done |
| 7 | Add `leftInStockForUpdate()` to `Warehouse` + `WarehouseJdbc`; update `CheckoutOrder` | ✅ Done |
| 8 | Run full test suite | ✅ PASS — 324 tests, 0 failures |

## 6. Decisions and approval

**Criteria revisions:** N/A — none  
**Implementation approval:** APPROVED — user (chat), run checkout-gaps-02, all 7 sub-tasks approved at plan review  
**External-write authorization:** NOT AUTHORIZED — changes remain in working tree; no commit or PR created
