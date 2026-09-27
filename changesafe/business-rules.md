# ChangeSafe Business Rules

<!-- LIVE REGISTER — never put placeholder rows here.
     Add a row only when a requester or authoritative document explicitly confirms a decision.
     Never infer CONFIRMED solely from current code, tests, or an assistant's guess.
     Read relevant entries on demand; do not inject the full file into every task. -->

## 1. Rules and sources

| Rule ID | Business rule | Scope | Source | Status | Confirmed by | Confirmed at |
| --- | --- | --- | --- | --- | --- | --- |
| BR-001 | Duplicate form submission with the same idempotency key must not create a second order | checkout | Code + test (PlaceOrderTest, OrderWorkFlowTest) | CONFIRMED | Code evidence | checkout-flow-01 |
| BR-002 | Cart is emptied only after all checkout writes succeed (transactional guarantee) | checkout | Code: CheckoutOrder @Transactional | CONFIRMED | Code evidence | checkout-flow-01 |
| BR-003 | Out-of-stock condition must prevent checkout entirely; partial fulfilment is not acceptable | checkout / warehouse | User decision Q-01, run checkout-flow-01 | CONFIRMED | User (chat) | checkout-flow-01 |
| BR-004 | After a DuplicateOrderException, the session token must be regenerated and the cart must be cleared, with an informative message shown to the user | checkout | User decision Q-02 + Q-03, run checkout-flow-01 | CONFIRMED | User (chat) | checkout-flow-01 |
| BR-005 | HTTPS is enforced in production; the CART_ID cookie must be set with HttpOnly, Secure, and SameSite=Strict flags | cart / security | User decision Q-04, run checkout-flow-01 | CONFIRMED | User (chat) | checkout-flow-01 |

## 2. Open questions

| Question ID | Question | Scope | Status | Answer / decision reference |
| --- | --- | --- | --- | --- |
| Q-05 | Should the catalog price be re-validated at checkout at the portal layer, or should a new domain use-case be introduced in the sales module? | checkout / catalog | OPEN | Blocks R-09 fix design |
| Q-06 | For R-07 (async listener failure handling): is a retry with fixed back-off acceptable, or does the team want an alerting/dead-letter mechanism? | billing / warehouse listeners | DEFERRED | R-07 deferred to a future run |

## 3. Decision history

| Decision ID | Rule / question IDs | Decision and reason | Source / confirmed by | Recorded at |
| --- | --- | --- | --- | --- |
| D-001 | Q-01 → BR-003 | Out-of-stock prevents checkout entirely. Partial fulfilment would silently fulfil incomplete orders with no consumer for GoodsMissed. | User (chat), run checkout-flow-01 | checkout-flow-01 |
| D-002 | Q-02 → BR-004 | After duplicate rejection: regenerate session token + clear cart (first order succeeded). User needs an informative message. | User (chat), run checkout-flow-01 | checkout-flow-01 |
| D-003 | Q-03 → BR-004 | Cart cleared on DuplicateOrderException because the first order was successfully placed. | User (chat), run checkout-flow-01 | checkout-flow-01 |
| D-004 | Q-04 → BR-005 | HTTPS enforced in production, so Secure cookie flag is appropriate. | User (chat), run checkout-flow-01 | checkout-flow-01 |
| D-005 | R-07 | Deferred: async listener failure handling requires infrastructure decisions (retry policy, DLQ) outside current run scope. | User decision DEFER, run checkout-flow-01 | checkout-flow-01 |
