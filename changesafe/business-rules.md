# ChangeSafe Business Rules

<!-- LIVE REGISTER — never put placeholder rows here.
     Add a row only when a requester or authoritative document explicitly confirms a decision.
     Never infer CONFIRMED solely from current code, tests, or an assistant's guess.
     Read relevant entries on demand; do not inject the full file into every task. -->

## 0. Process rules (ChangeSafe workflow)

These rules govern how ChangeSafe runs are conducted. They are not subject to requester override.

| Rule ID | Process rule | Source | Status |
| --- | --- | --- | --- |
| PR-001 | Every ChangeSafe run plan must include a final sub-task: "Generate evidence pack — fill `change-brief.md`, `risk-report.md`, `comparison.md`; update `plan.md` sub-task statuses; run `validate-output.ps1`". A run is not COMPLETE until this step is done. | Learned from checkout-gaps-02: evidence pack was skipped because no sub-task existed for it in the plan | CONFIRMED |

## 1. Rules and sources

| Rule ID | Business rule | Scope | Source | Status | Confirmed by | Confirmed at |
| --- | --- | --- | --- | --- | --- | --- |
| BR-001 | Submitting the same idempotency key twice must create exactly one order; the second attempt is rejected with a duplicate error, not a 500 | Checkout — order placement | checkout-flow-01: code + test evidence; change-brief BR-001 | CONFIRMED | User (chat), run checkout-flow-01 | 2026-09-26 |
| BR-002 | The cart is emptied only after all checkout writes succeed; if any write fails the cart must remain intact | Checkout — cart lifecycle | checkout-flow-01: CheckoutOrder @Transactional boundary | CONFIRMED | User (chat), run checkout-flow-01 | 2026-09-26 |
| BR-003 | Out-of-stock on any cart item prevents checkout entirely; partial fulfillment is not permitted | Checkout — stock validation | checkout-flow-01: Q-01, D-001 | CONFIRMED | User (chat), run checkout-flow-01 | 2026-09-26 |
| BR-004 | After a DuplicateOrderException: the session token must be regenerated, the cart must be cleared, and the user must see a clear duplicate-order message | Checkout — duplicate handling | checkout-flow-01: Q-02, Q-03, D-002, D-003 | CONFIRMED | User (chat), run checkout-flow-01 | 2026-09-26 |
| BR-005 | The CART_ID cookie must have HttpOnly=true, Secure=true (HTTPS), and SameSite=Strict flags | Cart — cookie security | checkout-flow-01: Q-04, D-004 | CONFIRMED | User (chat), run checkout-flow-01 | 2026-09-26 |
| BR-006 | The order total is always calculated from the current catalog price at checkout time; the price stored in the cart is never used for billing | Checkout — pricing | checkout-flow-01: Q-05 Option A; PlaceOrderFromCart re-prices via FindProducts | CONFIRMED | User (chat), run checkout-flow-01 | 2026-09-26 |
| BR-007 | The checkout session token is server-issued and session-bound; it serves as both the idempotency key and an implicit CSRF mitigation; it must not be replaced or removed without preserving both properties | Checkout — security | checkout-gaps-02: GAP-06 analysis; user confirmed via plan approval | CONFIRMED | User (chat), run checkout-gaps-02 | 2025-01-01 |
| BR-008 | Zero-total orders ($0.00) are valid; free or fully-discounted orders may be placed; no minimum order value is enforced | Checkout — pricing | checkout-gaps-02: GAP-04; explicit user decision at planning | CONFIRMED | User (chat), run checkout-gaps-02 | 2025-01-01 |
| BR-009 | Guest checkout is always permitted; a user does not need to be logged in to place an order | Checkout — authentication | checkout-gaps-02: GAP-04 analysis; current behavior confirmed as intentional | CONFIRMED | User (chat), run checkout-gaps-02 | 2025-01-01 |
| BR-010 | A guest who places an order is shown an order reference number on the success page so they can contact support; no email confirmation or self-service lookup endpoint is provided | Checkout — guest UX | checkout-gaps-02: GAP-08; explicit user decision at planning | CONFIRMED | User (chat), run checkout-gaps-02 | 2025-01-01 |
| BR-011 | The delivery address must be non-blank after whitespace stripping and at most 100 characters; a whitespace-only submission is invalid | Checkout — address validation | checkout-gaps-02: GAP-01; Place.java missing blank check identified | CONFIRMED | User (chat), run checkout-gaps-02 | 2025-01-01 |
| BR-012 | If any cart item references a product that no longer exists in the catalog, checkout must be rejected with a user-facing error message, not a 500 or a silent wrong total | Checkout — catalog integrity | checkout-gaps-02: GAP-03; PlaceOrderFromCart missing existence check identified | CONFIRMED | User (chat), run checkout-gaps-02 | 2025-01-01 |
| BR-013 | Stock is checked under a pessimistic lock (SELECT ... FOR UPDATE) at checkout to prevent two concurrent sessions from both passing the stock check for the last unit | Checkout — stock safety | checkout-gaps-02: GAP-02; explicit user decision (Option B) at planning | CONFIRMED | User (chat), run checkout-gaps-02 | 2025-01-01 |

## 2. Open questions

| Question ID | Question | Scope | Status | Answer / decision reference |
| --- | --- | --- | --- | --- |
| Q-01 | What is the async listener failure recovery strategy — retry policy, dead-letter queue, or alerting? | Post-checkout async fulfillment (R-07) | OPEN | Deferred; suggested follow-on run: checkout-async-resilience-01 |

## 3. Decision history

| Decision ID | Rule / question IDs | Decision and reason | Source / confirmed by | Recorded at |
| --- | --- | --- | --- | --- |
| D-001 | BR-003 | Out-of-stock blocks checkout entirely; no partial order or backorder path | User (chat), checkout-flow-01 Q-01 | 2026-09-26 |
| D-002 | BR-004 | On duplicate: regenerate session token so user can retry cleanly | User (chat), checkout-flow-01 Q-02 | 2026-09-26 |
| D-003 | BR-004 | On duplicate: clear cart because the original order already succeeded | User (chat), checkout-flow-01 Q-03 | 2026-09-26 |
| D-004 | BR-005 | CART_ID cookie flags required; HTTPS confirmed for production | User (chat), checkout-flow-01 Q-04 | 2026-09-26 |
| D-005 | BR-006 | Catalog price re-validation lives at portal layer via FindProducts (Option A) | User (chat), checkout-flow-01 Q-05 | 2026-09-26 |
| D-006 | BR-013 | Pessimistic lock (SELECT ... FOR UPDATE) chosen over accept-overselling or reservation table | User (chat), checkout-gaps-02 planning | 2025-01-01 |
| D-007 | BR-008 | $0 orders are valid; free/discounted orders are a supported use case | User (chat), checkout-gaps-02 planning | 2025-01-01 |
| D-008 | BR-010 | Guest order recovery: show reference number on success page only; no email or lookup endpoint | User (chat), checkout-gaps-02 planning | 2025-01-01 |
