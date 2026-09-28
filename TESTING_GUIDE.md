# Real-App Verification Guide — commit `8e48e53`

Manual test plan for the changes in *"Cancel the orders behind a failed payment, and
settle three response-contract bugs"*, plus the two pushed before it (`b70fea4`,
`7a05683`).

The automated suite is **112 tests, all passing**, but it is entirely
mock-based. It proves the branches are taken; it does not prove the behaviour is
right inside a real transaction, a real database, or against the real Razorpay
API. Everything below exists to close that gap.

---

## Why manual testing is not optional here

Three of these fixes are about state that spans multiple rows and multiple
requests, which mocks are structurally bad at representing:

1. **Failed payment → N orders cancelled.** The whole point is that one payment
   hold covering N orders leaves zero of them in `PLACED`. A mock with a
   hand-written list of three orders cannot show you that the *real* query
   actually returns three orders, nor that the cancellation actually commits.
2. **Order number collisions.** Requires a real unique index and real
   concurrency. The automated test proves ~77 bits of entropy; only a live
   table proves the column and index actually accept the new 19-character
   format.
3. **Refund correctness.** Depends on whether a hold was captured, which is only
   knowable from the payment module's own state.

---

## 0. Prerequisites

- Backend running locally with the `local` profile:
  ```powershell
  $env:SPRING_PROFILES_ACTIVE = "local"
  mvn spring-boot:run
  ```
- A real database (local Postgres) with Flyway migrations applied. Confirm it
  started clean:
  ```powershell
  Select-String -Path "logs\*.log" -Pattern "Flyway|Schema" | Select-Object -First 5
  ```
- Razorpay **test mode** keys. `application.yml` has defaults; override with
  `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` if you need your own account.
  > **Note:** `webhook-secret` defaults to a literal that ends in `=`. That is
  > fine for local tests only if it matches what you sign with (section 4.3
  > uses it directly). It is a committed credential placeholder — rotate before
  > this ever points at production.
- A customer account, at least two vendor shops with overlapping products, and
  a delivery address on file.

---

## 1. Order numbers are unique and fit the column

**What changed:** 6 random digits → `#DM-` + 15 random base-36 characters
(19 chars total, ~77 bits of entropy).

**The old code would fail this test roughly once per 90 orders at 10k orders.**
The new code should never fail it.

### 1.1 Format
Place a single order (section 2 walks through how). Then:
```powershell
$o = Invoke-RestMethod -Method Get -Uri "$base/api/customer/orders" -Headers $auth
$o.content[0].orderNumber
```
**Expect:** `#DM-` followed by exactly 15 uppercase alphanumeric characters.
Total length 19. Matches `^#DM-[0-9A-Z]{15}$`.

### 1.2 No collision under load
Insert 500 orders in a loop — this is where the old implementation would break:
```powershell
1..500 | ForEach-Object {
  # place a real order each iteration, or reuse a captured POST /api/customer/orders
}
```
Then check the database directly:
```sql
SELECT COUNT(*) AS total,
       COUNT(DISTINCT order_number) AS distinct_numbers
FROM orders
WHERE deleted_at IS NULL;
```
**Expect:** `total == distinct_numbers`. If they differ, **stop** — the unique
index is being violated somewhere and checkout is silently failing.

### 1.3 Concurrent checkouts do not collide
Open two browser profiles logged in as two different customers on the same
account-free setup, and have both check out simultaneously, repeatedly. The
real risk this covers is two application instances, which local single-node
testing cannot reproduce — so also run the backend in **two instances** (e.g.
ports 8080 and 8081 behind a load balancer, or via `SPRING_PROFILES_ACTIVE` in
two shells pointed at the same database) and hammer both:
```powershell
1..100 | ForEach-Object -Parallel {
  Invoke-RestMethod -Method Post -Uri "$using:base/api/customer/orders" `
    -Headers $using:auth -Body $using:body -ContentType "application/json"
} -ThrottleLimit 16
```
**Expect:** every request returns 200 or 4xx for a legitimate reason. A **500 on
one of the carts of a multi-cart checkout** is the old bug resurfacing — it
means the unique constraint was hit inside the transaction, which takes down
every order in that checkout, not just one.

### 1.4 The column accepts the length
The `order_number` column is `VARCHAR(20)` (migration `V42`). New numbers are 19
characters, so they fit, but confirm nothing truncated them:
```sql
SELECT order_number, LENGTH(order_number) AS len FROM orders
WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT 20;
```
**Expect:** all `len` values are 19. If any are 20+ or look truncated, the
column limit and the generator have drifted apart.

### 1.5 Note for support staff
Numbers **no longer sort chronologically** — that was the property of the
intermediate timestamp-based version, deliberately traded away. Anything that
was reading creation time out of the order number must now read `createdAt`.
Grep for such usage before shipping UI that sorts by number.

---

## 2. Multi-cart checkout baseline

**What changed** (from `b70fea4`): no empty carts, no fee-only orders.

Set up **three carts** by adding items that land in different shops. Verify
before checking out:
```powershell
Invoke-RestMethod -Method Get -Uri "$base/api/customer/carts" -Headers $auth
```
**Expect:** three carts, labelled `Cart 1` / `Cart 2` / `Cart 3`, each with a
non-zero total.

**Fee-only order test.** Add an item, then make it unavailable — simplest is to
have an admin deactivate the product or set its vendor's stock to 0:
```sql
UPDATE catalog_products SET is_active = false WHERE id = '<product-id>';
```
Reload the cart:
```powershell
Invoke-RestMethod -Method Get -Uri "$base/api/customer/carts" -Headers $auth
```
**Expect — two distinct outcomes, and it matters which one you get:**
- The whole cart is unavailable → that cart **disappears** from the response
  entirely. You are not shown a dead cart and never asked to pay for it.
- Only some items are unavailable → the cart **remains**, containing just the
  usable lines, and the removed lines are gone.

**Expect in both cases:** no cart has `total == 0`, and a **delivery fee is
never charged for a cart with nothing in it.** A fee attached to an empty cart
was the money bug in `b70fea4`.

Check out:
```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/customer/orders" -Headers $auth `
  -ContentType "application/json" -Body (@{ paymentMethodId = "COD" } | ConvertTo-Json)
```
**Expect:** one order per surviving cart, and one `PaymentOrder` covering all of
them. Each order response has a distinct `orderNumber` and a non-null
`sourceCartId`.

### 2.1 `itemCount` counts quantities
**What changed:** was line count, now sum of quantities.
```powershell
$o = Invoke-RestMethod -Method Get -Uri "$base/api/customer/orders" -Headers $auth
$o.content | Select-Object orderNumber, itemCount
```
Add one product with **quantity 3** before checking out.
**Expect:** `itemCount` is **3**, not 1. Cross-check against the cart badge
(`/api/customer/carts/count`), which counts quantities — the two must agree.
This matters because a mismatch is a customer-visible contradiction: the badge
said 3, the order says 1.

### 2.2 `sourceCartId` is populated
**Expect:** every order in a multi-cart checkout has a **distinct** non-null
`sourceCartId`, and each one matches the `id` of the cart it came from. If it is
null, a trace from an order back to its originating cart is impossible, which is
the whole reason it was added.

### 2.3 `paymentMethod` reflects the real method
**What changed:** was hardcoded to `"Credit Card"` for anything non-null.

Place one order per method — `COD`, `UPI`, `ONLINE`, `WALLET`:
```powershell
foreach ($m in @("COD","UPI","ONLINE","WALLET")) {
  $r = Invoke-RestMethod -Method Post -Uri "$base/api/customer/orders" -Headers $auth `
    -ContentType "application/json" -Body (@{ paymentMethodId = $m } | ConvertTo-Json)
  "$m -> $($r.paymentMethod)"
}
```
**Expect:** the returned `paymentMethod` **matches the method sent**, not
`"Credit Card"`. Before this fix, `UPI`, `ONLINE` and `WALLET` all displayed as
`"Credit Card"`, making them indistinguishable during support and reconciliation.

Also place an order with **no** `paymentMethodId`:
**Expect:** `COD` (the documented fallback), not `null` and not `"Credit Card"`.

---

## 3. Refunds and cancellation (from `7a05683`)

**What changed:** wallet credits no longer happen in the order module. Every
cancellation path delegates to `paymentService.onOrderCancelled(orderId)`, which
refunds only if the hold was actually captured.

### 3.1 Cancel a COD order
Place a COD order, then:
```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/customer/orders/$id/cancel" -Headers $auth
```
**Expect:** order is `CANCELLED`, customer gets one cancellation notification,
and the **wallet balance is unchanged** — nothing was ever charged.

### 3.2 Cancel an online order that WAS paid
Place an order, complete real payment in Razorpay test mode so the hold is
**captured**, then cancel.
**Expect:** exactly **one** refund for exactly the captured amount. Check:
```sql
SELECT * FROM wallet_transactions WHERE order_id = '<order-id>';
```
**Expect: one row.** The bug was a **double refund** — the order module credited
the total unconditionally *and* the payment module refunded. Two rows here means
you are running pre-`7a05683` code.

### 3.3 Cancel twice (idempotency)
Cancel the same order twice.
**Expect:** second call is a no-op. Still exactly one wallet transaction. No
second notification.

### 3.4 Vendor accept timeout
**What changed:** the timeout sweep now delegates to payment instead of
crediting the wallet itself.
Place an order and let the vendor-accept window lapse (or shorten
`vendor_accept_expires_at` in the DB to force it):
```sql
UPDATE orders SET vendor_accept_expires_at = NOW() - INTERVAL '1 hour'
WHERE id = '<order-id>';
```
Wait for the scheduled sweep to run.
**Expect:** order becomes `CANCELLED` with **no** wallet credit, since the hold
was never captured. If a refund appears here for an unpaid order, that is a
regression.

---

## 4. Failed payment cancels its orders — **the main fix**

**The bug:** `payment.failed` marked the `PaymentOrder` FAILED and stopped. One
checkout creates **one** hold across **N** orders, so all N stayed in `PLACED` —
a state a vendor can accept and delivery can complete. Settlements were paid to
vendors, drivers and the platform for orders nobody paid for.

This is the one that is hardest to verify by reading the code, because the
damage only appears when you follow the state all the way through.

### 4.1 Set up a 3-cart checkout
Create three carts as in section 2 and check out with an **online** method. Note:
- the `PaymentOrder` id (from the response, or `GET /api/payment/orders/{id}`)
- the three order ids and their `razorpay_order_id`

Confirm the starting state:
```sql
SELECT id, order_number, status, payment_method_id
FROM orders WHERE id IN ('<id1>','<id2>','<id3>');
```
**Expect: all three `PLACED`.**

### 4.2 Force the payment to fail
Use Razorpay test mode. Either:
- fail the payment at the checkout screen (use a failing test card / insufficient
  balance), **or**
- let the real webhook do it.

Prefer the **real webhook**, because the webhook path is what was broken. Point
a Razorpay test webhook at:
```
https://<your-host>/api/public/payment/webhook
```
event: `payment.failed`.

### 4.3 Send a signed `payment.failed` webhook manually
For a repeatable local test, craft the payload and sign it with the configured
webhook secret. The signature is HMAC-SHA256 **hex** of the exact raw body:
```powershell
$body = @'
{"id":"evt_test_failed_1","event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_test_1","order_id":"order_XXXXXXXXXXXX"}}}}
'@
$secret = $env:RAZORPAY_WEBHOOK_SECRET
if (-not $secret) { $secret = "YzT8kN2pQrX9vL4mB7wS1tR6uJ0hF3eA5cD8nK2xM4Q=" }

$hmac = [System.Security.Cryptography.HMACSHA256]::new(
  [System.Text.Encoding]::UTF8.GetBytes($secret))
$sig = [System.BitConverter]::ToString(
  $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($body))
).Replace("-","").ToLower()

Invoke-RestMethod -Method Post -Uri "$base/api/public/payment/webhook" `
  -Headers @{ "X-Razorpay-Signature" = $sig } `
  -ContentType "application/json" -Body $body
```
> The signature is over the **raw bytes**. Any reformatting — pretty-printing,
> changing whitespace, PowerShell's `-Body` encoding a BOM — yields a different
> signature. If you get `401 Signature invalid`, that is the cause, not a code bug.

Replace `order_XXXXXXXXXXXX` with the real `razorpay_order_id`.

### 4.4 Verify the outcome — the actual assertion
```sql
SELECT id, order_number, status, cancelled_at
FROM orders WHERE id IN ('<id1>','<id2>','<id3>');
```
**Expect: all three `CANCELLED`, all three with a non-null `cancelled_at`.**

**This is the single most important check in this document.** Pre-fix, this
query returned three `PLACED` rows and the money was gone.

```sql
SELECT id, status, razorpay_order_id FROM payment_orders WHERE id = '<payment-order-id>';
SELECT id, order_id, status, amount FROM payment_order_lines
WHERE payment_order_id = '<payment-order-id>';
```
**Expect:** payment `FAILED`; lines `VOIDED` — **not** `CANCELLED_REFUNDED`. The
hold was never captured, so a void is correct and a refund would be money from
nowhere.

```sql
SELECT COUNT(*) FROM wallet_transactions WHERE order_id IN ('<id1>','<id2>','<id3>');
```
**Expect: `0`.** A failed payment must never produce a refund.

### 4.5 Verify the settlement leak is actually closed
This is the part that proves the *impact* is gone, not just the status column.
With the three orders now `CANCELLED`, confirm the vendor/delivery path refuses
them:
```sql
SELECT * FROM vendor_order_candidates WHERE order_id IN ('<id1>','<id2>','<id3>');
```
**Expect: nothing actionable.** A cancelled order must not be acceptable,
dispatchable, or settleable. If a vendor can still accept one of these orders,
the payment side is fixed but the downstream settlement path is not.

### 4.6 Vendor no longer sees the order
Log in as a vendor and check the order list.
**Expect:** the three orders are gone. The customer saw "payment failed", so a
vendor accepting the same order would be accepting unpaid work.

### 4.7 Customer was told
**Expect:** exactly one cancellation notification per order (three total), each
naming its order number and citing the payment.

### 4.8 Idempotency — Razorpay retries
Send the **identical** webhook body again (same `evt_test_failed_1` event id).
**Expect:**
- no duplicate notifications
- no wallet transactions
- no state change
- no error in logs

Razorpay retries on non-2xx, so a non-idempotent handler would double-refund.

### 4.9 Webhook signature rejection
```powershell
# missing header
Invoke-RestMethod -Method Post -Uri "$base/api/public/payment/webhook" `
  -ContentType "application/json" -Body $body
# expect: 400

# wrong signature
Invoke-RestMethod -Method Post -Uri "$base/api/public/payment/webhook" `
  -Headers @{ "X-Razorpay-Signature" = "deadbeef" } `
  -ContentType "application/json" -Body $body
# expect: 401
```
**Expect:** neither request changes any order or payment state. The endpoint is
on `/api/public/**` and bypasses JWT auth, so the HMAC **is** the only thing
protecting it — confirm the rejection is real.

### 4.10 Failure of the order side does not lose the payment
Hard to force externally, but confirm by design in code: the payment is recorded
`FAILED` **before** the order module is called, and a throw from the handler is
caught and logged. If you break the handler deliberately, you should see
`Order-side reconciliation failed for payment <id>` in the log and the payment
still `FAILED` — with orders needing manual reconciliation. That log line is the
alarm; make sure it is actually monitored.

### 4.11 Top-up behaves differently
A wallet top-up covers **no** orders, so the handler is never invoked.
Top up the wallet and force a failure.
**Expect:** payment `FAILED`, wallet unchanged, and **no** order-side
reconciliation attempted. If the handler fires for a top-up, it is being called
with an empty list and will log misleadingly.

---

## 5. Ownership (from `7a05683`)

`verifyPayment` takes `userId` from the security context, not the request body.

Log in as customer A, then try to verify customer B's payment:
```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/customer/orders/verify-payment" `
  -Headers $authA -ContentType "application/json" -Body '{"paymentId":"<B-payment>"}'
```
**Expect: 403/404.** Pre-fix, passing another user's id in the body verified
*their* payment using *your* session.

Same check on `POST /api/payment/orders/verify`.

---

## 6. Payout webhook signature (from `7a05683`)

```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/public/payout/webhook" `
  -ContentType "application/json" -Body '{"event":"payout.processed"}'
# expect: 400 — missing signature
```
**Expect: rejected, and no payout marked processed.** This endpoint previously
accepted unsigned callbacks, which meant anyone who could reach it could mark
arbitrary payouts as processed.

---

## 7. Full regression sweep

The above covers the new work. Confirm nothing else broke — the suite covers
this, but these are the flows most likely to regress silently:

- [ ] Single-cart checkout still works end to end (multi-cart changes touched
      the shared code path).
- [ ] Selective checkout: `POST /api/customer/orders` with `cartIds` containing
      1 of 3 carts creates **one** order and leaves the other two carts open.
- [ ] Promo code applies and is removed, on a multi-cart cart.
- [ ] Wallet top-up, then spend the balance.
- [ ] Vendor accepts an order → prepares → delivery completes → payout created.
- [ ] Reorder from a past order.
- [ ] Order tracking endpoint returns live state.
- [ ] Soft-deleted rows never appear in any list response.

---

## 8. Known gaps — read before signing off

These are **not** covered above and are not covered by the automated suite
either. Be explicit about them if you are signing this off as production-ready.

1. **No real-database integration tests.** Everything is mock-based. The unique
   index behaviour in section 1 and the transaction boundaries in section 4 are
   verified by hand, not automatically. A Testcontainers-based suite would catch
   regressions permanently.
2. **Multi-instance is only partially verified.** The order-number fix was
   designed for it, but section 1.3 is a manual proxy. Nothing runs against a
   real load balancer in CI.
3. **The failed-payment reconciliation is not durable.** The order-side
   cancellation is an in-process call inside the webhook request. If the process
   dies between recording `FAILED` and cancelling the orders, the orders stay
   `PLACED` — the exact bug this fixes, just narrower. The log line in 4.10 is
   the only signal. A **transactional outbox** with a retrying consumer is the
   real fix and has not been built.
4. **No monitoring or alerting** is wired to the reconciliation failure log.
5. **Razorpay test-mode only.** Real-mode behaviour, including webhook
   behaviour under actual gateway retry schedules, is unverified.
6. **Committed credential placeholders** in `application.yml` (`razorpay` key
   id/secret, webhook secret, account number). Fine for local, must be rotated
   and moved to environment/secret storage before any real deployment.
