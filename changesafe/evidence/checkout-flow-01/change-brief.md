# ChangeSafe Change Brief — Checkout Flow Audit

| Field | Value |
| --- | --- |
| Run ID | checkout-flow-01 |
| Requester | NOT VERIFIED (user request via chat) |
| Requirement source | User request: "check the checkout flow any possible issue and enhancement needed" |
| Source state | 274af07a38bf541d8a434a34b375ffe1479b17b7 with historical uncommitted changes; exact diff NOT VERIFIED |
| Status | READY FOR APPROVAL |

## 1. Problem and goal

**Problem:** No specific defect was reported. The requester asked for a broad audit of the checkout
flow to identify latent issues and enhancements before any production release.

**Goal:** Implement the nine approved checkout risk mitigations and verify the eight agreed acceptance criteria within the available evidence.

## 2. Scope and exclusions

**In scope:**
- `OrderController.place()` — idempotency token lifecycle, validation, duplicate handling
- `CheckoutOrder.checkout()` — transactional coordinator
- `PlaceOrderFromCart.placeOrder()` — cart → order conversion, price re-validation (R-09)
- `PlaceOrderJdbc.place()` — idempotency SELECT-then-INSERT race fix (R-04)
- `CheckoutOrder` — stock pre-flight check (R-05), cart clear on duplicate (R-03)
- `CartIdFromCookies` — cookie security flags (R-06)
- `CartJdbc.items()` — in-memory cache stale-read (R-08)
- `order-error.html` + `messages.properties` — error message code whitelist (R-10)
- New test additions for all YES risks

**Out of scope:** R-07 (async listener error handling — DEFERRED). Catalog browsing, user
registration/login, payment gateway, admin/backoffice, deployment infrastructure.

## 3. Business rules and open questions

| Rule ID | Confirmed rule | Source / decision |
| --- | --- | --- |
| BR-001 | Duplicate idempotency key must not create a second order | Code + test evidence |
| BR-002 | Cart emptied only after all checkout writes succeed | Code: CheckoutOrder @Transactional |
| BR-003 | Out-of-stock prevents checkout entirely; no partial fulfilment | User Q-01, D-001 |
| BR-004 | After DuplicateOrderException: regenerate token, clear cart, show informative message | User Q-02 + Q-03, D-002 + D-003 |
| BR-005 | CART_ID cookie requires HttpOnly, Secure, SameSite=Strict; HTTPS confirmed in prod | User Q-04, D-004 |

| Question ID | Question for requester | Answer / status | Blocks |
| --- | --- | --- | --- |
| Q-05 | Should catalog price re-validation live at portal layer or a new sales use-case? | ANSWERED — requester chose Option A (portal layer) | N/A — design answered |
| Q-06 | Async listener failure: retry vs DLQ? | DEFERRED (R-07 deferred) | N/A this run |

## 4. Acceptance criteria

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | Submitting the same idempotency key twice creates exactly one order row and returns 302 | Existing + new integration test |
| AC-02 | After DuplicateOrderException: cart is cleared, session token is refreshed, user sees duplicate message | New controller + integration test |
| AC-03 | Concurrent double-submit with same key: second thread gets graceful 302/error, not 500 | New PlaceOrderTest for DataIntegrityViolationException |
| AC-04 | Checkout blocked when any cart item is out of stock | New CheckoutOrder + integration test |
| AC-05 | CART_ID cookie has HttpOnly, Secure (in prod), SameSite=Strict flags | New CartIdFromCookies unit test |
| AC-06 | CartJdbc.items() always reads from DB (no stale in-process cache) | Existing CartTest + new mutation test |
| AC-07 | order-error.html only renders known message codes; unknown code falls back to default | New controller test |
| AC-08 | Client-supplied cart price is re-validated against catalog price at checkout | New PlaceOrderFromCart test (pending Q-05 design decision) |

## 5. Implementation and verification plan

| Step | Planned action | Dependency / checkpoint |
| --- | --- | --- |
| 1 | Fix R-04: catch DataIntegrityViolationException in PlaceOrderJdbc and re-throw as DuplicateOrderException | Phase 8 approval |
| 2 | Fix R-01 + R-03: on DuplicateOrderException in OrderController — clear cart, remove + regenerate session token | Phase 8 approval |
| 3 | Fix R-02: validate client idempotencyKey == session checkoutToken in OrderController.place() | Phase 8 approval |
| 4 | Fix R-05: add pre-flight stock check in CheckoutOrder.checkout() before placeOrder() | Phase 8 approval + BR-003 |
| 5 | Fix R-06: add HttpOnly, Secure, SameSite=Strict to CART_ID cookie in CartIdFromCookies | Phase 8 approval + BR-005 |
| 6 | Fix R-08: remove in-memory items cache from CartJdbc.items() | Phase 8 approval |
| 7 | Fix R-09: re-validate cart prices against catalog at checkout in PlaceOrderFromCart | Phase 8 approval + Q-05 design |
| 8 | Fix R-10: whitelist valid message codes in OrderController.error(); order-error.html falls back | Phase 8 approval |
| 9 | Add i18n messages for new error codes | Phase 8 approval |
| 10 | Run before-fix and after-fix test gates | Phase 7 / Phase 9 |

## 6. Decisions and approval

**Criteria revisions:** N/A — none
**Implementation approval:** APPROVED — user (chat), run checkout-flow-01, R-09 design Option A (portal layer via FindProducts)
**External-write authorization:** NOT AUTHORIZED
