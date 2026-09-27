# Cart Quantity Feature & Flow Audit — Plan

## Top-Level Overview

**Goal:** Audit the full add-to-cart → checkout flow for correctness and missing business rules,
then add a new feature that lets users increase or decrease item quantities directly on the
cart page using `+`/`-` buttons and a single "Update cart" form POST.

**Scope:**
- Read the existing cart and checkout flow end-to-end and document missing business rules.
- Add a new `POST /cart/update` endpoint to the `CartController` that accepts a map of
  `productId → newQuantity` and applies absolute quantities (remove-then-add strategy).
- Add an accumulated-quantity ceiling guard in `CartController.addItem()`: if the existing
  stored quantity + the delta would exceed 1000, reject the add with a validation error.
- Convert `GET /cart/remove` → `POST /cart/remove` to prevent CSRF/prefetch mutations;
  update the Remove link in `cart.html` to a small form POST.
- Update `cart.html` to render quantity controls (`-`, number input, `+`) and an
  "Update cart" submit button as a traditional form POST.
- No AJAX for the update path — traditional form POST → redirect back to `/cart`.
- No changes to the domain layer (`CartJdbc`, `Cart`, `CartItem`) or the checkout flow.

**Confirmed UX decisions:**
- `+`/`-` buttons with an editable number input; one "Update cart" button commits all changes.
- Setting quantity to 0 removes the item (equivalent to the existing Remove link).
- No AJAX on update — full page redirect after POST.

---

## Flow Audit Findings

### Current flow (trace from code evidence)

| Step | Endpoint / Method | Key Logic | Known Gaps |
|------|-----------------|-----------|------------|
| 1. Add to cart (AJAX) | `POST /cart/items` → `CartController.addWithoutRedirect` | Validates qty 1–1000; `FindProducts.byId` checks product exists; `CartJdbc.add` increments or inserts | No upper-bound guard on *accumulated* cart quantity per item (e.g., 1000 clicks → 1000 qty, then another add of 1 succeeds and reaches 1001) |
| 2. Add to cart (form) | `POST /cart` → `CartController.add` | Same internal `addItem()` path | Same gap as above |
| 3. View cart | `GET /cart` → `CartController.index` | Queries DB fresh every time (no cache); maps to `{id, title, price, quantity}` — `price` here is `item.total()` (unit × qty), not unit price | `price` key in the model is actually the *line total*, not the unit price — this is correct for display but the naming is misleading in the model map |
| 4. Remove item | `GET /cart/remove?productId=…` | Deletes row for that productId and cartId | Uses GET to perform a state mutation — vulnerable to CSRF / accidental prefetch. No confirmed rule to change this, so document as a finding only. |
| 5. Checkout start | `GET /order` → `OrderController.index` | Generates a session-bound checkout token (idempotency key + implicit CSRF) | None observed |
| 6. Place order | `POST /order` → `OrderController.placeOrder` | Validates name/address; `CheckoutOrder.checkout` (stock check under pessimistic lock → re-price from catalog → place → prepare delivery → empty cart) | No maximum cart size guard; very large carts could create very many order_items rows in a single transaction. Low severity. |
| 7. Order success | `GET /order/success` | Shows orderId flash for guest (BR-010) | None |
| 8. Order error | `GET /order/error` | Renders sanitized i18n message code | None |

### Missing / unconfirmed business rules — decisions recorded

| Finding ID | Area | Observation | Decision | New Rule |
|------------|------|-------------|----------|----------|
| F-001 | Cart quantity ceiling | No guard on the *total accumulated* quantity per item. Single-add is bounded at 1–1000, but repeated adds push stored qty above 1000. | **CONFIRMED: cap at 1000 per item.** `addItem()` must check existing qty + delta ≤ 1000 before writing. | BR-014 |
| F-002 | Cart remove via GET | `GET /cart/remove` performs a destructive mutation; vulnerable to CSRF/prefetch. | **CONFIRMED: convert to POST.** Remove link in `cart.html` becomes a form POST. | BR-015 |
| F-003 | Cart quantity update (new feature) | No rule defines valid range for the update form. | **CONFIRMED: 1–1000 per item; 0 = remove item.** Same ceiling as add. | Part of BR-014 |
| F-004 | Quantity primitive lower bound | `Quantity` allows 0; no domain-level guard against `Quantity(0)` being passed to `CartJdbc.add`. | Low severity; document only — no code change in this run. | — |
| F-005 | Cart expiry / abandonment | Abandoned `cart_items` rows accumulate indefinitely. | Out of scope; added as open question to `business-rules.md`. | — |

---

## Sub-Tasks

---

### Sub-Task 1 — Document flow audit and record confirmed business rules

**Status:** `[x] done`

**Intent:**
Produce the ChangeSafe evidence directory, fill the change-brief from the template,
and record the five confirmed decisions (F-001 to F-005) as new business rules and
open questions in the live register. All decisions are now confirmed by the user.

**Expected Outcomes:**
- Directory `changesafe/evidence/cart-qty-01/` created.
- `changesafe/evidence/cart-qty-01/change-brief.md` filled from template, status DRAFT.
- `changesafe/business-rules.md` updated with:
  - **BR-014**: Cart quantity per item is capped at 1000 (accumulated + delta ≤ 1000); update form allows 1–1000 absolute; 0 removes the item.
  - **BR-015**: `POST /cart/remove` replaces `GET /cart/remove` to prevent CSRF/prefetch mutations.
  - Open question added for F-005 (cart expiry/abandonment — out of scope, future run).

**Todo List:**
1. Create directory `changesafe/evidence/cart-qty-01/`.
2. Copy `changesafe/templates/change-brief.md` to `changesafe/evidence/cart-qty-01/change-brief.md`.
3. Fill the change-brief: run ID `cart-qty-01`, source state = current HEAD, ACs listed,
   implementation plan referencing sub-tasks 2–4, findings F-001 to F-005 recorded.
4. Add BR-014 and BR-015 rows to `changesafe/business-rules.md`.
5. Add F-005 as an open question to the open questions table in `changesafe/business-rules.md`.

**Relevant Context:**
- [`changesafe/business-rules.md`](changesafe/business-rules.md) — live rule register.
- [`changesafe/templates/change-brief.md`](changesafe/templates/change-brief.md) — template.
- [`src/main/java/com/ttulka/ecommerce/portal/web/CartController.java`](src/main/java/com/ttulka/ecommerce/portal/web/CartController.java)
- [`src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java`](src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java)
- [`src/main/java/com/ttulka/ecommerce/common/primitives/Quantity.java`](src/main/java/com/ttulka/ecommerce/common/primitives/Quantity.java)

---

### Sub-Task 2 — Add `POST /cart/update`, convert Remove to POST, add qty ceiling in `CartController`

**Status:** `[x] done`

**Intent:**
Three coordinated changes to `CartController`:
1. New `POST /cart/update` endpoint — sets absolute quantity per item using remove-then-add.
2. Convert `GET /cart/remove` → `POST /cart/remove` (BR-015).
3. Add accumulated-quantity ceiling check in `addItem()` — reject if existing qty + delta > 1000 (BR-014).

**Expected Outcomes:**
- `CartController` has a new `@PostMapping("/update")` method.
  - Accepts form params `quantity_<productId>` (one per cart item).
  - For each param: if `quantity == 0` → `cart.remove(productId)`; if `1 ≤ quantity ≤ 1000` → `cart.remove(productId)` then `cart.add(new CartItem(…, new Quantity(quantity)))`; otherwise → `IllegalArgumentException`.
  - Redirects to `GET /cart` after processing all pairs.
- `GET /cart/remove` endpoint removed; replaced by `POST /cart/remove` (`@PostMapping("/remove")`) accepting `productId` form param.
- `addItem()` queries the current stored quantity for the product before calling `cart.add()`, and throws `IllegalArgumentException` if `existingQty + delta > 1000`.
- No changes to the domain layer (`CartJdbc`, `Cart`, `CartItem`).

**Todo List:**
1. Read `CartController.java` to identify insertion and modification points.
2. In `addItem()`: before calling `cart.add()`, fetch the cart's current items, find the matching `productId`, and check `existingQty + quantity > 1000` — throw `IllegalArgumentException("Cart quantity limit reached")` if so.
3. Change `@GetMapping("/remove")` to `@PostMapping("/remove")`; update method signature to accept `@NonNull String productId` as a form param (already present).
4. Add `@PostMapping("/update")` method:
   - Accept `@RequestParam Map<String, String> params` (keys `quantity_<productId>`).
   - Parse each entry, validate range, call `cart.remove` then `cart.add` for non-zero, or `cart.remove` only for zero.
   - Redirect to `/cart`.
5. Write or extend test coverage for:
   - `POST /cart/update` valid — quantity changes correctly.
   - `POST /cart/update` quantity = 0 — item removed.
   - `POST /cart/update` quantity > 1000 — rejected.
   - Accumulated add ceiling — add that would push item past 1000 is rejected.
   - `POST /cart/remove` removes item correctly.

**Relevant Context:**
- [`src/main/java/com/ttulka/ecommerce/portal/web/CartController.java`](src/main/java/com/ttulka/ecommerce/portal/web/CartController.java:56) — existing POST mapping pattern.
- [`src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java`](src/main/java/com/ttulka/ecommerce/sales/cart/jdbc/CartJdbc.java:65) — `add()` and `remove()`.
- [`src/main/java/com/ttulka/ecommerce/sales/cart/Cart.java`](src/main/java/com/ttulka/ecommerce/sales/cart/Cart.java) — Cart interface.
- Convention: method names `snake_case` in tests; controller method names follow existing camelCase.

---

### Sub-Task 3 — Update `cart.html` with quantity controls, Update cart form, and POST Remove

**Status:** `[x] done`

**Intent:**
Replace the static `"Qty N"` display and the `<a>` Remove link with interactive controls:
- `−` / number input / `+` quantity controls per row.
- A single "Update cart" form POST to `POST /cart/update`.
- Each "Remove" link converted to a small inline form POSTing to `POST /cart/remove` (BR-015).

**Expected Outcomes:**
- Each cart row shows: `[−] [qty input] [+]` alongside item title; input name is `quantity_<productId>`.
- The quantity controls and "Update cart" button are wrapped in one `<form action="/cart/update" method="post">`.
- The "Remove" link is replaced by a `<form action="/cart/remove" method="post">` with a hidden `productId` input and a submit button styled like the current remove link.
- A single "Update cart" submit button is placed outside the `th:each` loop, inside the wrapping form.
- Page degrades without JS (form still submits correctly).
- Thymeleaf `th:each` loop updated; layout decorator fragment not broken.
- New i18n keys added to `messages.properties`: `cart.update`, `cart.qty.decrease`, `cart.qty.increase`.

**Todo List:**
1. Read `src/main/resources/templates/cart.html` and `src/main/resources/messages.properties`.
2. Wrap the `.cart-items` div (inside `.checkout-grid`) in `<form action="/cart/update" method="post">`.
3. In each `th:each` row:
   - Replace `<span th:text="'Qty ' + ${item.quantity}">` with:
     `<button type="button" data-qty-dec>−</button>`
     `<input type="number" th:name="'quantity_' + ${item.id}" th:value="${item.quantity}" min="0" max="1000">`
     `<button type="button" data-qty-inc>+</button>`
   - Replace the `<a class="remove-link">` with a `<form action="/cart/remove" method="post">` containing a hidden `<input name="productId">` and a styled submit button.
4. Add an "Update cart" `<button type="submit">` after the `th:each` div, inside the update form.
5. Add minimal JS (inline `<script>` at the bottom of the template or in `cart.js`) to wire `data-qty-dec` / `data-qty-inc` to their adjacent inputs. Must not interfere with `cart.js` AJAX logic.
6. Add i18n keys `cart.update`, `cart.qty.decrease`, `cart.qty.increase` to `messages.properties`.
7. Verify both empty-cart and non-empty-cart states render correctly.

**Relevant Context:**
- [`src/main/resources/templates/cart.html`](src/main/resources/templates/cart.html)
- [`src/main/resources/messages.properties`](src/main/resources/messages.properties)
- [`src/main/resources/static/cart.js`](src/main/resources/static/cart.js) — existing JS; new JS should be in a separate small block or the same file if appropriate.
- Layout decorator: `layout:decorate="~{layout/default}"` — do not break the fragment structure.

---

### Sub-Task 4 — Write and run tests; generate evidence pack

**Status:** `[x] done`

**Intent:**
Ensure the new feature has adequate test coverage and produce the ChangeSafe evidence
pack for this run.

**Expected Outcomes:**
- New or updated tests in the appropriate test class cover:
  - `POST /cart/update` with valid quantities updates the cart correctly.
  - `POST /cart/update` with quantity = 0 removes the item.
  - `POST /cart/update` with quantity > 1000 is rejected.
  - Existing add/remove tests still pass.
- `changesafe/evidence/cart-qty-01/risk-report.md` filled from the template.
- `changesafe/evidence/cart-qty-01/comparison.md` filled.
- `validate-output.ps1` run and passes.
- `bob_sessions/INDEX.md` updated with this run.

**Todo List:**
1. Identify the existing `CartController` test class (search for `CartControllerTest` or
   equivalent `@WebMvcTest`/`@SpringBootTest` test targeting cart endpoints).
2. Add test cases for the three `POST /cart/update` scenarios listed above.
3. Run targeted tests:
   ```
   changesafe/scripts/run-targeted-tests.ps1 -RunId cart-qty-01 -Tests "CartControllerTest" -Phase after-fix
   ```
4. Fill `changesafe/evidence/cart-qty-01/risk-report.md`.
5. Fill `changesafe/evidence/cart-qty-01/comparison.md`.
6. Run `changesafe/scripts/validate-output.ps1 -RunDir "changesafe/evidence/cart-qty-01"`.
7. Update `bob_sessions/INDEX.md`.

**Relevant Context:**
- [`src/test/java/com/ttulka/ecommerce/`](src/test/java/com/ttulka/ecommerce/) — test root.
- Project convention: test method names in `snake_case`; `@JdbcTest` for JDBC slices;
  full `@SpringBootTest` for HTTP-level tests. Use `@ActiveProfiles("test")`.
- [`changesafe/templates/risk-report.md`](changesafe/templates/risk-report.md)
- [`changesafe/templates/comparison.md`](changesafe/templates/comparison.md)
- [`changesafe/scripts/validate-output.ps1`](changesafe/scripts/validate-output.ps1)

---

## Confirmed Decisions (all open questions resolved)

| ID | Decision |
|----|----------|
| OQ-1 | **YES** — cap accumulated per-item quantity at 1000. `addItem()` checks existing + delta ≤ 1000. → BR-014 |
| OQ-2 | **Convert to POST** — `GET /cart/remove` → `POST /cart/remove`; Remove links become form POSTs. → BR-015 |
| OQ-3 | **Remove-then-add** — no domain layer changes; `CartController.update()` calls `cart.remove()` then `cart.add()` to set absolute quantity. |

---

## Implementation Order

```
Sub-Task 1 → Sub-Task 2 → Sub-Task 3 → Sub-Task 4
```

Sub-Tasks 2 and 3 may proceed in either order since `CartController` and `cart.html` are
independent files. Sub-Task 4 must run last.
