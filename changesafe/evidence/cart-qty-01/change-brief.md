# ChangeSafe Change Brief — Cart Quantity Update Feature & Flow Audit

| Field | Value |
| --- | --- |
| Run ID | cart-qty-01 |
| Requester | User (chat) |
| Requirement source | User chat request — add-to-cart → checkout flow audit + cart quantity increase/decrease feature |
| Source state | commit 13058af71c88f816930bca37c7644ae836799be9 (branch: changesafe/test-case-04) |
| Status | DRAFT |

## 1. Problem and goal

**Problem:** The cart page shows a static quantity label and a "Remove" link only — users cannot
adjust item quantities without removing and re-adding. Additionally, the flow audit identified two
security and correctness gaps: (1) repeated add-to-cart clicks can push the stored quantity above
1000 with no ceiling check, and (2) the Remove action uses a GET request, which is vulnerable to
CSRF and accidental browser prefetch.

**Goal:** Add +/− quantity controls with an "Update cart" form POST to the cart page; enforce a
1000-unit accumulated quantity ceiling per item; and convert Remove to a POST request — all with
no changes to the domain layer.

## 2. Scope and exclusions

**In scope:**
- `CartController`: new `POST /cart/update` endpoint (absolute quantity, remove-then-add strategy);
  accumulated quantity ceiling check in `addItem()`; convert `GET /cart/remove` → `POST /cart/remove`.
- `cart.html`: `[−][input][+]` controls per row; single "Update cart" form POST; Remove link → form POST.
- `messages.properties`: new i18n keys for update/quantity controls.
- `changesafe/business-rules.md`: add BR-014, BR-015, and F-005 open question.
- Tests for all new controller paths.
- Evidence pack (risk-report, comparison, validate-output).

**Out of scope:**
- Domain layer changes (`CartJdbc`, `Cart`, `CartItem`, `Quantity` primitive).
- Checkout flow changes.
- Cart expiry / abandoned-cart cleanup (F-005 — deferred).
- AJAX update path (traditional form POST confirmed).
- Email or order-lookup changes.

## 3. Business rules and open questions

| Rule ID | Confirmed rule | Source / decision |
| --- | --- | --- |
| BR-014 | Cart quantity per item is capped at 1000 (accumulated + delta ≤ 1000); the update form allows 1–1000 absolute; setting to 0 removes the item | User confirmed OQ-1, cart-qty-01 planning |
| BR-015 | `POST /cart/remove` replaces `GET /cart/remove` to prevent CSRF/prefetch-triggered item deletion | User confirmed OQ-2, cart-qty-01 planning |

| Question ID | Question for requester | Answer / status | Blocks |
| --- | --- | --- | --- |
| Q-01 | F-005: Should abandoned cart_items rows be cleaned up (TTL, scheduled job, or session expiry hook)? | OPEN — deferred to future run cart-expiry-01 | Cart expiry feature; out of scope for this run |

## 4. Acceptance criteria

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | Clicking +/− on the cart page changes the quantity input value client-side without submitting the form | Manual / template inspection |
| AC-02 | Submitting "Update cart" with a valid quantity (1–1000) updates the cart item and redirects to `/cart` | Integration test / `POST /cart/update` |
| AC-03 | Submitting "Update cart" with quantity = 0 removes the item from the cart | Integration test / `POST /cart/update` |
| AC-04 | Submitting "Update cart" with quantity > 1000 is rejected with a validation error | Integration test / `POST /cart/update` |
| AC-05 | Adding an item whose accumulated stored quantity would exceed 1000 is rejected | Integration test / `POST /cart/items` or `POST /cart` |
| AC-06 | Clicking "Remove" sends a `POST /cart/remove` (not a GET) and removes the item | Template inspection + integration test |
| AC-07 | All existing cart add/remove tests continue to pass | Maven test gate |

## 5. Implementation and verification plan

| Step | Planned action | Dependency / checkpoint |
| --- | --- | --- |
| 1 | Create evidence dir, fill change-brief, add BR-014 + BR-015 to business-rules.md | N/A |
| 2 | `CartController`: add qty ceiling in `addItem()`; convert remove to POST; add `POST /cart/update` | BR-014, BR-015 confirmed; OQ-3 remove-then-add confirmed |
| 3 | `cart.html`: quantity controls, update form, POST remove; `messages.properties` i18n keys | Sub-Task 2 endpoint contract (param name `quantity_{productId}`) |
| 4 | Tests for AC-02 to AC-07; run Maven test gate; fill risk-report + comparison; run validate-output | All prior steps complete |
| last | Generate evidence pack — fill `change-brief.md`, `risk-report.md`, `comparison.md`; update plan sub-task statuses; run `validate-output.ps1` | Phase 11; all prior steps complete |

## 6. Decisions and approval

**Criteria revisions:** N/A — none  
**Implementation approval:** Full scope approved by user (chat, cart-qty-01 planning session). Authorized to edit `CartController.java`, `cart.html`, `messages.properties`, and test files.  
**External-write authorization:** NOT AUTHORIZED — no commit/push/PR authorized in this run.
