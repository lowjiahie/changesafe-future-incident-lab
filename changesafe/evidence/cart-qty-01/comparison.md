# ChangeSafe Before / After — Cart Quantity Update Feature & Flow Audit

| Field | Value |
| --- | --- |
| Run ID | cart-qty-01 |
| Goal | Add +/− quantity controls to cart page; enforce accumulated qty ceiling (BR-014); convert Remove to POST (BR-015) — no domain layer changes |
| Safety invariant | For any cart item, the stored quantity after any addItem() call must never exceed 1000 |
| Before-fix state | commit 13058af71c88f816930bca37c7644ae836799be9 (branch: changesafe/test-case-04) |
| After-fix state | working tree — CartController.java, cart.html, messages.properties, layout/default.html, CartControllerTest.java |
| Project conventions | [.bob/rules/01-project-conventions.md](../../../.bob/rules/01-project-conventions.md) — DRAFT; no deviations |
| Change brief | [change-brief.md](change-brief.md) |
| Comparison captured at | 2025-01-01T16:01:00+08:00 |

## Before / after results

| Measure | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test (R-01 ceiling) | Test did not exist; no ceiling in `addItem()` — repeated adds accumulate above 1000 | PASS — HTTP 400 returned when existing qty + delta > 1000 | `CartControllerTest.add_rejects_when_accumulated_quantity_exceeds_limit` |
| Focused safety test (R-02 POST remove) | GET mapping — CSRF risk; test used GET | PASS — POST mapping only; `formaction` in template | `CartControllerTest.item_is_removed_from_the_cart` (updated) |
| New endpoint tests | Not present | 3/3 PASS: update valid qty, update qty=0 removes, update qty>1000 rejected | `CartControllerTest` — 5 new tests total |
| Relevant test suite | 3 CartControllerTest PASS | 8 CartControllerTest PASS (3 existing + 5 new) | `./mvnw test -Dtest="CartControllerTest"` |
| Full suite | N/A (baseline) | 328 / 0 / 0 (run / failures / errors) | `./mvnw test` — BUILD SUCCESS |
| Cart page — qty display | Static `Qty N` label | `[−][input][+]` controls per row | `cart.html` lines 20–31 |
| Cart page — update | No update form | `POST /cart/update` form with "Update cart" button | `cart.html` lines 14–44 |
| Cart page — remove | `a[href=/cart/remove]` GET link | `button[formaction=/cart/remove]` POST | `cart.html` line 38–41 |
| Execution time | N/A | 8.7 s (full suite) | Maven console |

## Goal check

| ID | Acceptance criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | +/− buttons change qty input client-side | MET | JS in `cart.html` scripts fragment |
| AC-02 | POST /cart/update valid qty updates cart | MET | `update_changes_item_quantity_in_cart` PASS |
| AC-03 | POST /cart/update qty=0 removes item | MET | `update_with_quantity_zero_removes_item` PASS |
| AC-04 | POST /cart/update qty>1000 rejected 400 | MET | `update_rejects_quantity_above_limit` PASS |
| AC-05 | Accumulated qty ceiling enforced on add | MET | `add_rejects_when_accumulated_quantity_exceeds_limit` PASS |
| AC-06 | Remove uses POST | MET | `item_is_removed_from_the_cart` PASS; `formaction` in template |
| AC-07 | All existing tests pass | MET | 328/328 full suite PASS |

**Overall goal:** MET — all seven acceptance criteria are met with direct test or code evidence.

## What the evidence proves

The accumulated quantity ceiling (BR-014) is enforced at the HTTP layer in `CartController.addItem()` and verified by a new regression test that confirms HTTP 400 is returned when the stored quantity plus the requested delta would exceed 1000. The Remove action (BR-015) has been converted to a POST-only endpoint, verified by an updated test that asserts a POST request is required for a successful redirect.

## Remaining risk

R-04 (cart expiry/abandonment) is deferred to a future run (`cart-expiry-01`). R-03 (`Quantity(0)` domain primitive gap) is WONT-FIX — the HTTP layer is sufficient for production paths. Neither is in scope for this run and neither blocks release of the quantity-update feature.

## Presentation takeaway

The cart quantity update feature is fully implemented with test coverage: users can now adjust item quantities directly on the cart page, the system enforces a 1000-unit ceiling per item, and Remove is CSRF-safe.
