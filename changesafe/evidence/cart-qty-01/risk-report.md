# ChangeSafe Risk Report — Cart Quantity Update Feature & Flow Audit

| Field | Value |
| --- | --- |
| Run ID | cart-qty-01 |
| Generated at | 2025-01-01T16:01:00+08:00 |
| Requirement / change | Add +/− quantity controls to cart page, enforce accumulated qty ceiling, convert Remove to POST |
| Before-fix source state | commit 13058af71c88f816930bca37c7644ae836799be9 (branch: changesafe/test-case-04) |
| After-fix source state | working tree (uncommitted changes in CartController.java, cart.html, messages.properties, CartControllerTest.java, layout/default.html, business-rules.md) |
| Bob task ID / title | cart-qty-01 — Cart Quantity Update Feature & Flow Audit |
| Project rule | [.bob/rules/01-project-conventions.md](.bob/rules/01-project-conventions.md) — DRAFT (no app-code generation blocked; only controller + template changes) |
| Change brief | [change-brief.md](change-brief.md) |
| Business decisions | BR-014, BR-015 — see [changesafe/business-rules.md](../../../changesafe/business-rules.md) |
| Report status | COMPLETE |

## 1. Goal and scope

**Goal:** Add +/− quantity controls to the cart page with a single "Update cart" form POST, enforce a 1000-unit per-item accumulated quantity ceiling (BR-014), and convert `GET /cart/remove` to `POST /cart/remove` (BR-015) — with no changes to the domain layer.

| ID | Acceptance criterion | Verification method |
| --- | --- | --- |
| AC-01 | +/− buttons change the quantity input client-side without submitting the form | Template inspection — `data-qty-dec`/`data-qty-inc` JS in `cart.html` |
| AC-02 | POST /cart/update with valid quantity (1–1000) updates cart and redirects to /cart | `CartControllerTest.update_changes_item_quantity_in_cart` — PASS |
| AC-03 | POST /cart/update with quantity = 0 removes the item | `CartControllerTest.update_with_quantity_zero_removes_item` — PASS |
| AC-04 | POST /cart/update with quantity > 1000 is rejected with 400 | `CartControllerTest.update_rejects_quantity_above_limit` — PASS |
| AC-05 | Add whose accumulated qty would exceed 1000 is rejected with 400 | `CartControllerTest.add_rejects_when_accumulated_quantity_exceeds_limit` — PASS |
| AC-06 | Remove button sends POST /cart/remove (not GET) | `CartControllerTest.item_is_removed_from_the_cart` (updated to POST) — PASS; `cart.html` uses `formaction="/cart/remove"` |
| AC-07 | All existing cart add/remove tests continue to pass | Full suite: 328 tests, 0 failures — PASS |

**In scope:** `CartController.java`, `cart.html`, `messages.properties`, `layout/default.html` (scripts fragment), `CartControllerTest.java`, `changesafe/business-rules.md`, evidence files.

**Out of scope:** Domain layer (`CartJdbc`, `Cart`, `CartItem`, `Quantity`), checkout flow, cart expiry (F-005 — Q-02 open).

**Unresolved business questions:** Q-02 — cart expiry/abandonment cleanup (does not block this run).

## 2. Cheap test gate

| Selected tests | Command | Result | Exit code | Duration (s) | Summary / raw log |
| --- | --- | --- | --- | --- | --- |
| CartControllerTest | `./mvnw test -Dtest="CartControllerTest"` | PASS | 0 | 1.4 | 8 tests, 0 failures, 0 errors |
| Full suite | `./mvnw test` | PASS | 0 | 8.7 | 328 tests, 0 failures, 0 errors |

**Reason to continue:** All 8 CartControllerTest tests pass (including 5 new tests); full suite green at 328/328.

## 3. Change-impact map

| Component | Direct / downstream | Observed path or dependency | Evidence |
| --- | --- | --- | --- |
| `CartController` | Direct | New `POST /cart/update` endpoint; qty ceiling in `addItem()`; `GET /cart/remove` → `POST /cart/remove` | [`CartController.java`](../../../src/main/java/com/ttulka/ecommerce/portal/web/CartController.java) |
| `CartJdbc` / `Cart` domain | Downstream (unchanged) | `cart.remove()` + `cart.add()` used for remove-then-add in update endpoint | No domain changes; [`CartJdbc.java`](../../../src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java) |
| `cart.html` template | Direct | Quantity controls rendered; update form POST; Remove via `formaction` | [`cart.html`](../../../src/main/resources/templates/cart.html) |
| `layout/default.html` | Direct | Added `layout:fragment="scripts"` slot for page-specific JS | [`default.html`](../../../src/main/resources/templates/layout/default.html) |
| `messages.properties` | Direct | 3 new i18n keys: `cart.update`, `cart.qty.decrease`, `cart.qty.increase` | [`messages.properties`](../../../src/main/resources/messages.properties) |
| Checkout flow | No impact | Cart emptied by `CheckoutOrder.checkout()` — unchanged; no new fields or schema changes | N/A |
| Database schema | No impact | No DDL changes; no migration file needed | N/A |

## 4. Future incidents

| Risk ID | Severity | Trigger and incident | User / system impact | Evidence | Confidence | Fix decision | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R-01 | HIGH | Repeated `POST /cart` or `POST /cart/items` before this fix: accumulated qty exceeds 1000 per item, corrupting expected order size | User places unintentionally large orders; order total inflated; warehouse stock incorrectly depleted | [`CartJdbc.add()`](../../../src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java:65) increments unconditionally; no pre-existing ceiling check | HIGH | YES | PREVENTED |
| R-02 | MEDIUM | `GET /cart/remove` link before this fix: CSRF-crafted URL or browser prefetch silently removes cart items | User loses cart items without intent; poor UX and potential lost sale | [`CartController.java`](../../../src/main/java/com/ttulka/ecommerce/portal/web/CartController.java) — was `@GetMapping`; no CSRF token | HIGH | YES | PREVENTED |
| R-03 | LOW | `Quantity` primitive allows 0; `CartJdbc.add(new CartItem(…, new Quantity(0)))` would insert a zero-quantity row; only HTTP layer guards this | Stale zero-quantity row in DB if called programmatically bypassing controller | [`Quantity.java`](../../../src/main/java/com/ttulka/ecommerce/common/primitives/Quantity.java:18) — `quantity >= 0` | LOW | WONT-FIX | WONT-FIX — domain-level guard out of scope; HTTP layer sufficient for production path |
| R-04 | LOW | Abandoned `cart_items` rows accumulate indefinitely — no TTL or cleanup mechanism | DB bloat over time; no user-visible impact in short term | [`CartJdbc.java`](../../../src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java) — no expiry logic | MEDIUM | DEFER | DEFERRED — open question Q-02; suggested run: cart-expiry-01 |

**Risks fixed in this run:** R-01, R-02  
**Risks deferred to backlog:** R-04  
**Risks marked WONT-FIX:** R-03 (domain primitive guard — HTTP layer sufficient)  
**Selection rationale:** R-01 selected as primary safety contract — highest severity (HIGH), directly exploitable by normal user behavior (repeated clicks), and fully verifiable by existing test infrastructure.

## 5. Selected safety contract

| Field | Value |
| --- | --- |
| Linked risk | R-01 |
| Safety invariant | For any cart item, the stored quantity after any `addItem()` call must never exceed 1000 |
| Adverse condition | User or client submits repeated add-to-cart requests for the same product |
| Reproduction / regression test | [`CartControllerTest.add_rejects_when_accumulated_quantity_exceeds_limit`](../../../src/test/java/com/ttulka/ecommerce/portal/web/CartControllerTest.java) |
| Expected result | HTTP 400 returned; `cart.add()` not called; existing stored quantity unchanged |
| Actual before-fix result | NOT RUN — pre-fix baseline not captured (no CartControllerTest for this scenario existed); risk confirmed by code evidence (no ceiling check in `addItem()`) |
| Actual after-fix result | HTTP 400 returned — confirmed by `CartControllerTest.add_rejects_when_accumulated_quantity_exceeds_limit` PASS |

## 6. Implementation and verification

| Item | Before fix | After fix | Evidence |
| --- | --- | --- | --- |
| Focused safety test (R-01 ceiling) | N/A — test did not exist | PASS | [`CartControllerTest.add_rejects_when_accumulated_quantity_exceeds_limit`](../../../src/test/java/com/ttulka/ecommerce/portal/web/CartControllerTest.java) |
| Focused safety test (R-02 POST remove) | item_is_removed used GET — would still pass on GET | PASS — updated to POST assertion | [`CartControllerTest.item_is_removed_from_the_cart`](../../../src/test/java/com/ttulka/ecommerce/portal/web/CartControllerTest.java) |
| New update endpoint tests | N/A | 3 new tests PASS | update_changes_item_quantity_in_cart, update_with_quantity_zero_removes_item, update_rejects_quantity_above_limit |
| Relevant existing tests | 3 cart controller tests PASS | 8 cart controller tests PASS (3 existing + 5 new) | `CartControllerTest` — 8/8 |
| Full test suite | N/A (baseline) | 328/328 PASS, 0 failures | `./mvnw test` output |
| Application behavior — add | No ceiling; repeated adds accumulate above 1000 | Ceiling enforced; HTTP 400 on overflow | `CartController.addItem()` lines 82–91 |
| Application behavior — remove | GET mutation; CSRF risk | POST mutation; formaction in template | `CartController` `@PostMapping("/remove")`; `cart.html` `formaction` |
| Application behavior — update | No update endpoint | `POST /cart/update` applies absolute qty via remove-then-add | `CartController.update()` |
| Template | Static Qty label; anchor Remove link | `[−][input][+]` controls; Update form; formaction Remove | `cart.html` |

**Approved code changes:**
- [`CartController.java`](../../../src/main/java/com/ttulka/ecommerce/portal/web/CartController.java) — qty ceiling in `addItem()`; `POST /cart/remove`; `POST /cart/update`; `@ExceptionHandler(IllegalArgumentException.class)` → 400
- [`cart.html`](../../../src/main/resources/templates/cart.html) — quantity controls, update form, formaction remove
- [`layout/default.html`](../../../src/main/resources/templates/layout/default.html) — `layout:fragment="scripts"` slot
- [`messages.properties`](../../../src/main/resources/messages.properties) — 3 new keys
- [`CartControllerTest.java`](../../../src/test/java/com/ttulka/ecommerce/portal/web/CartControllerTest.java) — 5 new tests; updated remove test to POST

**Implementation approval:** User (chat), full scope approved during cart-qty-01 planning session.  
**Project conventions followed:** No banned suffixes used; `@RequiredArgsConstructor` + `@NonNull` retained; no domain layer changes; test method names `snake_case`; `@WebMvcTest` slice for controller tests; `IllegalArgumentException` guard follows existing `addItem()` pattern.  
**Approved deviations:** N/A — none.  
**Review findings:** N/A — `/review` not run (no PR diff available; working tree changes only).

## 7. Goal check and remaining risk

| ID | Criterion | Status | Observation and evidence |
| --- | --- | --- | --- |
| AC-01 | +/− buttons change qty input client-side without submitting | MET | JS in `cart.html` scripts fragment wires `data-qty-dec`/`data-qty-inc` |
| AC-02 | POST /cart/update valid qty updates cart and redirects | MET | `update_changes_item_quantity_in_cart` PASS |
| AC-03 | POST /cart/update qty=0 removes item | MET | `update_with_quantity_zero_removes_item` PASS |
| AC-04 | POST /cart/update qty>1000 rejected 400 | MET | `update_rejects_quantity_above_limit` PASS |
| AC-05 | Add that pushes accumulated qty past 1000 rejected | MET | `add_rejects_when_accumulated_quantity_exceeds_limit` PASS |
| AC-06 | Remove uses POST | MET | `item_is_removed_from_the_cart` PASS (POST); `cart.html` `formaction="/cart/remove"` |
| AC-07 | All existing tests pass | MET | 328/328 PASS |

**Overall goal:** MET — all seven acceptance criteria are met with direct test or code evidence.  
**Remaining risk (in-scope, not fixed):** N/A — all in-scope risks addressed. R-03 (Quantity(0) domain gap) WONT-FIX; R-04 (cart expiry) DEFERRED.  
**Bob advisory recommendation:** Proceed — all ACs met, full suite green, no new failures introduced. Manual browser testing recommended before production release to verify `formaction` behavior across browsers and the JS +/− controls.

### Deferred risk backlog

| Risk ID | Severity | Summary | Reason deferred | Suggested follow-on run ID |
| --- | --- | --- | --- | --- |
| R-04 | LOW | Abandoned cart_items rows accumulate indefinitely; no TTL or cleanup mechanism | Out of scope for this run; requires separate product decision on cleanup strategy | cart-expiry-01 |

**Human go/no-go decision:** ____________________  
**Reviewer and date:** ____________________

## 8. Evidence provenance

| Artifact | Path / task reference | Captured at |
| --- | --- | --- |
| Before-fix test log | NOT RUN — no pre-fix baseline captured; risk confirmed by code evidence | N/A |
| After-fix test log | `changesafe/evidence/cart-qty-01/logs/` (Maven console output; no script log) | 2025-01-01T16:01:00+08:00 |
| Migration check | N/A — no schema changes | N/A |
| Before/after comparison | [comparison.md](comparison.md) | 2025-01-01T16:01:00+08:00 |
| Change brief | [change-brief.md](change-brief.md) | 2025-01-01T16:01:00+08:00 |
| Evaluation hints | N/A — no hints supplied | N/A |
| Bob consumption screenshot | NOT VERIFIED — capture manually and save under `bob_sessions/` | N/A |
