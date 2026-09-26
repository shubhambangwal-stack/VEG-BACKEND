# VegGo Fresh — Project State (living doc)

Paste this back into the chat any time context feels lost — it's the full picture of
what's built, what's stubbed, what's genuinely missing, and what's been **redesigned but
not yet coded**, across every module touched so far.

**⚠️ READ THIS FIRST:** Section "NEW ARCHITECTURE — locked design, not yet built" below
describes a real pivot away from parts of what's already coded (mainly: vendor-set-own-price
→ Admin-owned catalog; single vendor per order assumption → multi-cart split). Treat that
section as the source of truth for anything it touches, even where it contradicts the
"as built" sections below it.

---

## Module status

| Module | Status | Package | Flyway range used |
|---|---|---|---|
| Platform | Frozen (Phase 0) | `com.veggofresh.platform` | V1–V19 |
| Auth | Built by team, seen not edited | `com.veggofresh.auth` | V20–V39 (but V6, V7 also used here — see gaps) |
| Customer | Built by team, seen not edited | `com.veggofresh.customer` | V40–V69 (but V8 also used here — see gaps) |
| Vendor | Fully built (pre-pivot model) — **needs rework for new catalog model** | `com.veggofresh.vendor` | V70–V79 used, V80–V89 free |
| Delivery | Fully built (pre-pivot model) — **needs pickup-OTP + broadcast rework** | `com.veggofresh.delivery` | V90–V102 used, V103–V109 free |
| Admin | Not started — **next module to build, everything now depends on it** | `com.veggofresh.admin` | V110–V129 |
| Payment | Not started — **design fully locked** (Razorpay authorize/hold/capture/void, Wallet for all 3 user types, sequential multi-order checkout). Only real bank withdrawal/payout stays stubbed for now. | `com.veggofresh.payment` | V130–V149 |
| Notification | Not started | `com.veggofresh.notification` | V150–V169 |

---

## NEW ARCHITECTURE — locked design, not yet built

This is the result of a full architecture conversation that happened after Delivery and
Vendor were built against the *original* model (vendor sets own price/description,
customer orders from one vendor's listing directly, one order = one vendor always). The
new model changes vendor-side product ownership, cart behavior, order broadcast
mechanics, and adds a wallet system. **None of this is coded yet.** Everything below is
locked/confirmed with the user, not speculative.

### 1. Catalog ownership flips to Admin

- Admin owns the **master product catalog**: name, description, category/subcategory,
  **one fixed platform-wide price**, and **product image**.
- Vendors **no longer create products or set their own price/description**. Vendors
  select which catalog products they carry and toggle availability.
- Vendor "stock" is **binary for now**: `isListed` (available / not available) — **not**
  quantity-tracked. Real, final confirmation of "I actually have this" happens at
  **order-accept time** — a vendor can be listed and still decline if they discover
  they're out.
  - Quantity-based stock (`stockQuantity`, low-stock thresholds — what's currently built
    in `VendorInventoryService`/`InventoryItem`) is **deferred as a future feature**,
    not removed from the roadmap. Current code keeps working for now but the
    matching/broadcast logic under the new model will only check `isListed`.
- New products a vendor wants to carry but don't exist in the catalog yet: **vendor
  submits a request → Admin reviews/approves** before it becomes addable. This needs a
  new Admin-owned workflow/table.

### 2. Cart — multi-cart model (this replaces the earlier "disable incompatible
   products" idea, which was discussed and then explicitly overturned)

- Cart-building is **vendor-aware**. Adding a product checks existing open carts **in
  order** (Cart 1, then Cart 2, then Cart 3...) for vendor overlap with the new item:
  - Overlap found with a cart → item joins that cart, that cart's candidate-vendor-set
    **narrows to the intersection**.
  - No overlap with any existing cart → a **new cart is created** for that item, with
    its own fresh candidate-vendor-set.
- **Carts are static once formed** — no recompute or re-merge when an item is later
  removed (confirmed simplification; can leave carts slightly more fragmented than
  strictly necessary post-removal, accepted as a fair trade for not needing constant
  recomputation).
- **Customer sees the split while shopping** — "Cart 1 / Cart 2 / ..." clearly shown —
  but checks out with **one single payment**. That one payment fans out into **N
  independent orders** behind the scenes (own broadcast, own atomic accept, own
  delivery cycle, potentially different couriers), shown to the customer as separate
  shipment/delivery tracking cards after checkout.
- **Revisit-after-a-delay edge case**: candidate-vendor-sets are computed at add-time
  using live data, but a vendor could stop carrying an item or go offline before
  checkout. At **checkout time**, every cart's item-vendor overlap is **re-validated
  fresh**. A cart whose overlap has since broken does **not** block the other carts —
  it's flagged individually ("Some items in this group are no longer available nearby
  — remove them to continue, or we'll leave them out of this order") while the rest of
  checkout proceeds normally.
- **Per-add cost, for reference** (not a performance concern, just documenting the
  mechanism): each add-to-cart does one indexed query (vendors stocking this product,
  `isListed = true`, within customer's radius) + in-memory set-intersection against
  each currently-open cart's stored candidate set. Cheap because the candidate set is
  stored and narrowed incrementally on the cart itself, never recomputed from scratch.

### 3. Order → Vendor broadcast

- Order broadcasts **simultaneously** to every vendor in the (already narrowed, already
  re-validated) candidate-vendor-set for that cart/order — **not** staggered
  notifications (staggering was proposed, then explicitly rejected — it doesn't prevent
  concurrent accept writes, it just delays some vendors for no real safety benefit).
- Race condition is resolved with an **atomic DB-level conditional accept** — e.g.
  `UPDATE ... SET status = 'VENDOR_ACCEPTED' WHERE id = ? AND status = 'PENDING'`,
  relying on `BaseEntity`'s existing `@Version` optimistic-locking column, with the
  resulting conflict actually caught and turned into a clean "already taken" response
  instead of a raw 500. **This exact bug already exists today** in Delivery's current
  `acceptAssignment()` — no atomic guard, so two partners could theoretically both
  succeed. Needs fixing as part of this work regardless, not just for the new vendor
  broadcast.
- Because carts guarantee single-vendor fulfillability by construction (multi-cart model
  above), **every order has exactly one vendor, always** — no partial/split-fulfillment
  logic needed at the order level itself. (This was the resolution to an earlier, more
  complex "grouped bundle broadcast" idea that was explored and dropped as
  overengineered for now.)

### 4. Vendor → Delivery dispatch

- Dispatch trigger moves from **"vendor accepts"** to **"vendor marks ready for
  pickup"** — this was an open question in the original build; now resolved.
- Same simultaneous-broadcast + atomic-accept pattern as above, this time to delivery
  partners within radius of the vendor.

### 5. Two-sided OTP verification (new)

- **Pickup**: vendor issues an OTP to the delivery partner at handoff; partner enters it
  to confirm pickup. **This does not exist yet** — net-new, separate from drop
  verification.
- **Drop**: OTP + photo — **already built** in Delivery module, unchanged.
- Live status + map tracking visible to both customer and vendor throughout, matching
  what Customer's `OrderTrackingResponseDto` already partially supports.

### 6. Payment module — fully locked design (real Razorpay integration, not deferred)

Payment module is now a **real, first-class module**, not a stub — this reverses the
earlier "mostly deferred" framing. It owns: checkout payment collection (Razorpay),
Wallet (for all three user types — Customer, Vendor, Delivery), payouts to Vendor/
Delivery (Razorpay Route, real but can follow after core checkout+wallet is working),
and platform commission configuration. `Payment` is the natural owner of Wallet — this
was an open question, now resolved: **Wallet lives in Payment module**, exposed to
Customer/Vendor/Delivery via a `WalletService` interface, same cross-module pattern as
everything else (no other module imports Payment's entities directly).

**Checkout payment model — authorize/hold, capture on vendor accept:**

- **Rejected first**: pay-on-delivery (cash/UPI-at-door) was seriously considered and
  explicitly rejected. Reasoning: it removes all financial commitment from the customer
  until the very last step, after a real vendor has already prepped goods and a real
  delivery partner has already traveled to pick them up — meaning a frivolous/prank
  order wastes two other people's real time and money with zero recourse. Rejected
  specifically because this platform's model (broadcast to independent vendor +
  independent delivery partner, not one integrated restaurant) makes that exposure
  worse than a typical food-delivery app's.
- **Locked model**: Razorpay **authorize/hold at checkout, capture on vendor accept,
  void on timeout.**
  - Order created via Razorpay Orders API with manual capture (`payment_capture: 0`).
    Customer pays (card, UPI Intent/QR, netbanking, wallet-app — all supported under
    standard capture per Razorpay docs); money is *authorized* (held) but not yet
    settled to VegGo Fresh.
  - **Vendor accepts** → backend calls Razorpay's Capture API → money actually moves.
  - **Vendor-accept timeout expires with nobody accepting** → backend calls Void →
    hold released, customer's money simply becomes available again on their end, no
    refund transaction needed (nothing was ever captured).
  - Razorpay's own hard ceiling: uncaptured authorized payments auto-refund
    automatically after **5 days** regardless (merchant-configurable shorter window
    also available) — a safety net, not something the design relies on, since real
    timeouts here will be minutes.
  - **UPI note**: UPI **Collect** flow (manual VPA entry) is deprecated by Razorpay/NPCI
    as of Feb 28 2026 (already past as of project's current date) — integration should
    use **UPI Intent** or **UPI QR** instead, not Collect.
- **On later failure** (vendor accepted, i.e. payment already captured, but delivery
  partner never accepts or delivery otherwise fails) → **full refund to Wallet**, not
  back to the original payment method. Funded from platform's own settled funds, since
  Razorpay had already captured and settled by that point. This was already locked
  earlier and is unchanged.

**Wallet — first-preference, partial-payment, sequential multi-order model:**

- Exists for **all three user types**: Customer (spend + withdraw), Vendor (earnings +
  withdraw), Delivery (earnings + withdraw). Same underlying ledger mechanism for all
  three, just different crediting events (customer: refunds; vendor/delivery: order
  payouts).
- **At checkout, one single checkbox**: "Use wallet balance" (on/off) — applies to the
  whole checkout, not a manually-typed amount, not per-order. Simple by design.
- Recall: one checkout can produce **multiple independent orders** (one per finalized
  cart, per the multi-cart model in section 2). Wallet application across these is
  **sequential**: wallet balance is applied to Cart 1's order first (up to its total),
  any remaining wallet balance spills into Cart 2's order, then Cart 3's, in the same
  order the carts were originally formed. Whatever each order still needs after its
  wallet portion goes through the Razorpay hold above — so a single order can be part
  wallet + part Razorpay, and a sibling order in the same checkout might be 100%
  Razorpay if wallet ran out before reaching it, or 100% wallet if it was covered
  entirely.
- **Both the wallet portion and the Razorpay portion behave identically as "holds"** —
  symmetric mechanics, this is the key design point:
  - Wallet's portion of the order total is **soft-reserved** at checkout (moved from
    "available" to "reserved" in the ledger — not a real debit yet), mirroring
    Razorpay's authorize/hold state exactly.
  - **On that order's vendor accept** → wallet's reserved portion finalizes into a real
    debit (permanent ledger entry) at the same moment the Razorpay portion gets
    captured.
  - **On that order's timeout (no vendor accepts)** → wallet's reserved portion
    releases back to "available" balance (no ledger entry needed, nothing was ever
    really taken) at the same moment the Razorpay hold gets voided.
- **Each order's fate is fully independent of its siblings from the same checkout** —
  confirmed explicitly: e.g. Cart 1 might be wallet-only and time out (releases back to
  wallet), while Cart 2 in the same checkout is wallet+Razorpay and gets accepted
  (finalizes both portions) — nothing about Cart 1's outcome affects Cart 2's
  resolution or vice versa, regardless of which cart drew from wallet first.
- **No automatic retry** — a released/failed order's reserved funds just become normal
  spendable wallet balance again. If the customer wants that item, they place a brand
  new order manually; it goes through the exact same checkout flow (checkbox,
  sequential fill, hold-until-accept) as any other order. No special-case "retry" logic
  anywhere.
- **Actual bank withdrawal** (moving wallet balance out to a real bank account, for all
  three user types) can stay **stubbed** for now, same pattern already used for
  Vendor/Delivery payout screens — only the in-app balance/ledger + spend-at-checkout
  needs to be real and working now.

**Vendor/Delivery payouts & platform commission:**

- Vendor and Delivery income (from completed orders) settles into their respective
  Wallets. **Platform commission/service-fee percentage is Admin-configurable**, not
  hardcoded — this **replaces** the flat 10% placeholder currently sitting in Vendor's
  order-detail response (`VendorOrderManagementService`), which was always flagged as a
  made-up number.
- Real bank payout (Razorpay Route / linked accounts) can follow once core
  checkout+wallet is working — bank-detail collection during Vendor/Delivery onboarding
  was already deliberately shaped to be Route-compatible (legal business name, business
  type/category, address, email, phone, IFSC + account number + beneficiary name) for
  exactly this reason.

**Admin-configurable timeouts — new Admin responsibility:**

- **Vendor-accept timeout** and **delivery-accept timeout** are both Admin-configurable
  (not hardcoded), independently of each other.
- **Hard upper bound enforced in code** (not just documented) on whatever Admin sets —
  e.g. capped at something like 30 minutes max — specifically so a misconfigured
  Admin setting can't accidentally hold a customer's card/wallet funds for an
  unreasonably long time. This stays far inside Razorpay's own 5-day ceiling regardless,
  but the sane cap is enforced defensively at the application layer, not left to that
  outer limit.

### 7. Scheduled orders

- `DeliverySlot`/`scheduledDate` already exist in Customer's code (`OrderRequestDto`),
  but Delivery module currently assumes immediate dispatch and has no awareness of
  scheduling at all.
- Confirmed behavior: **vendor broadcast happens immediately** at order placement
  (locking in a vendor even for a future-dated order); **delivery broadcast is deferred**
  until closer to the scheduled slot.

### 8. Vendor & Delivery broadcast lifecycle — fully locked edge-case design

This covers exactly what happens at every accept/cancel/timeout point across both the
vendor-broadcast and delivery-broadcast legs, including failure interaction with the
Payment design above. Locked in detail because these are the actual race conditions and
retry loops the whole broadcast model depends on getting right.

**Accept race (applies identically to vendor-broadcast and delivery-broadcast):**
- The instant one accept succeeds (atomic DB-level conditional write, per section 3),
  the order is **immediately removed from every other eligible party's queue**.
- Whoever's accept write loses the race sees an explicit message —
  **"Someone else already accepted this order"** — not a silent disappearance. Applies
  the same way on both the vendor side and the delivery-partner side.

**Vendor cancels after accepting:**
- Order **re-broadcasts** to remaining eligible vendors, **excluding** the vendor who
  just cancelled (and excluding anyone else who's already declined/cancelled this
  specific order in an earlier round — exclusion accumulates across rounds).
- Bounded by **two admin-configurable limits**, whichever is hit first: **max number of
  re-broadcast rounds**, and **max total elapsed time** across all rounds combined (not
  just per-round). Both numbers are set by Admin, independently of the vendor-accept
  and delivery-accept per-round timeouts (section 6).
- If either limit is hit with nobody accepting → order is cancelled, **full refund to
  Wallet**.

**Delivery-partner cancels after accepting ("ready for pickup" already broadcast):**
- Same re-broadcast pattern, same exclusion rule, same two-limit admin-configurable
  bound, same outcome on limit-hit (cancel + Wallet refund) as the vendor-cancel case
  above.
- If a "ready for pickup" broadcast round simply times out with **nobody** accepting
  (not a cancel-after-accept, just nobody responds) — same re-broadcast/bound/refund
  path applies, not a different one.

**Payment/accept ordering — capture must succeed for accept to be final:**
- A vendor's "Accept" is only truly final once Razorpay **capture actually succeeds**
  (see section 6 — capture is triggered by vendor-accept). Three things need to happen
  together the moment a vendor taps accept: (a) remove the order from every other
  vendor's queue, (b) trigger Razorpay capture + finalize the Wallet reservation, (c)
  generate the pickup OTP.
- **If capture fails for any reason** (insufficient funds, card declined, anything) —
  this collapses to **exactly the same outcome as "nobody accepted in time"**: no
  deduction, no wallet finalization, the vendor's accept is reversed, order fails
  cleanly. This is a **terminal failure**, not a retry case — **no re-broadcast** is
  attempted for a capture failure specifically, since the problem is the customer's
  payment itself, not vendor availability. (Distinct from the timeout/cancel cases
  above, which *do* re-broadcast — the difference is whether the underlying problem is
  "nobody available" [retry-worthy] vs. "the payment didn't actually work" [not
  retry-worthy].)

**OTP standard:**
- **Both** pickup-OTP (new, vendor-issues/partner-enters) and drop-OTP (existing,
  already built) are **6 digits**. Drop-OTP was originally shorter in the existing
  Delivery build — needs aligning to 6 digits for consistency when this gets
  implemented.
- **No attempt cap, no lockout, on either OTP** — explicit decision, not an oversight.
  (Flagged as a real tradeoff during design: no attempt cap means an OTP is in theory
  brute-forceable via script in seconds since it's just a numeric code with unlimited
  tries. Decision made anyway, knowingly, rather than adding attempt-cap complexity —
  revisit only if this becomes an actual observed problem in practice.)

- **Staggered (1-second-gap) notification to avoid races** — rejected; doesn't address
  concurrent writes, only delays some recipients.
- **"Disable" incompatible products in a single cart** — this was the original fix for
  the multi-product-fragmentation problem; superseded by the multi-cart model in section 2.
- **Post-checkout "grouped bundle" broadcasting** (greedy set-cover: find the vendor
  covering the most remaining items, broadcast the biggest bundle first, repeat) — this
  was scoped in detail as the "hybrid" option before the multi-cart-at-build-time model
  was proposed and preferred instead, because it avoids ever needing multi-round,
  stateful, timeout-driven broadcasting. Documented here as a rejected alternative, not
  because it was wrong, but because building fragmentation-avoidance at cart-time is
  simpler than fixing it after checkout.

---

## Rework required on already-built modules (because of the pivot above)

This is not a full re-audit — it's what's obviously implied by the new design. A real
pass is still needed once Admin exists and cart/order logic starts getting rebuilt.

**Vendor module:**
- `VendorProductController`/`VendorProductService` (vendor creates products with own
  name/price/description) — **conflicts with new model**. Needs to become "select from
  Admin catalog + toggle `isListed`" instead.
- `InventoryItem`/`VendorInventoryService` (`stockQuantity`, `deductStock`,
  `lowStockThreshold`) — stays in codebase, but matching/broadcast logic under the new
  model will ignore quantity and check `isListed` only. Not removed, just not load-bearing
  for order matching anymore, until quantity tracking is revisited later.
- Vendor's order-accept flow needs the atomic conditional-accept treatment (section 3),
  with an explicit "someone else already accepted" response for the losing party
  (section 8) — not a silent failure.
- Cancel-after-accept needs the re-broadcast loop from section 8 (exclude the
  cancelling vendor + any prior decliners, bounded by admin-configured max
  rounds/elapsed time, then cancel + Wallet refund).
- Accept is only final once Razorpay capture succeeds (section 8) — a capture failure
  reverses the vendor's accept and fails the order cleanly, no re-broadcast attempted
  for that specific case (distinct from the availability-timeout case, which does
  re-broadcast).
- New: vendor-side "mark ready for pickup" action needs to actually trigger
  `DeliveryDispatchService` (this was already the identified integration seam before the
  pivot — still true, just now fires at a different lifecycle point than "accept"), and
  needs to generate the pickup-OTP at accept-time (section 8) for the delivery partner
  to use later.
- New: vendor-side product-catalog-request flow (request a product Admin hasn't added
  yet).

**Delivery module:**
- New pickup-OTP flow (vendor-issued, partner-entered) — additive, doesn't break
  anything existing. **6 digits, no attempt cap** (locked, section 8).
- Existing drop-OTP needs aligning from its current digit count to **6 digits** for
  consistency with the new pickup-OTP (locked, section 8).
- `acceptAssignment()` needs the same atomic conditional-accept fix as Vendor (section 3)
  — this is a real existing bug, not just new-feature scope. Loser of the race needs an
  explicit "someone else already accepted" response, not a silent failure (section 8).
- Dispatch trigger timing moves to "vendor ready for pickup" — `DeliveryDispatchService`
  interface likely doesn't need to change shape, just *when* it gets called from Vendor.
- Cancel-after-accept and pure-timeout-with-no-accept both need the re-broadcast loop
  from section 8 (exclude prior decliners, bounded by admin-configured max
  rounds/elapsed time, then cancel + Wallet refund).

**Customer module:**
- Cart (`CartService`/`CartServiceImpl`, `Cart`/`CartItem` entities) needs the multi-cart
  model — this is a real structural change: a customer can have **multiple concurrent
  carts**, not one. `CartItemRequestDto`'s current shape (add to *the* cart) needs
  rethinking against "add, and the system decides which cart it lands in or creates a
  new one."
- `OrderRequestDto`/checkout needs to handle **one payment producing N orders** (one per
  finalized cart), not one order per checkout call.
- Existing promo-code bug (discount computed on cart, never applied at checkout —
  `OrderServiceImpl.checkout()` hardcodes `promoDiscount = ZERO`) still needs fixing
  regardless of this pivot — unrelated bug, flagged earlier, not yet fixed.
- Rating fragmentation (Customer's own `Rating`, `VendorShopRating`, `DeliveryPartnerRating`
  all disconnected — `rateOrder()` only touches the first) still needs an orchestration
  decision — unrelated to this pivot, not yet fixed.
- `OrderRepository.findByShopId` directly references Vendor's `Product` entity in JPQL —
  boundary issue, symmetric to Vendor's own violations, not yet fixed, and will likely
  need rethinking anyway once products are catalog-based rather than vendor-owned.

---

## Delivery module — full endpoint inventory (as built, pre-pivot)

**Onboarding** (`/api/delivery/onboarding`): `GET /status`, `PUT /basic-info`,
`POST /verification/step-1` (multipart, license), `POST /verification/step-2` (multipart,
vehicle), `PUT /verification/step-3` (bank details, JSON)

**Profile** (`/api/delivery`): `GET /profile`, `GET /profile/stats`, `POST /profile`,
`POST /kyc-documents` (legacy), `PUT /status` (online/offline, starts/stops active-hours session)

**Documents** (`/api/delivery/documents`): `GET /`, `POST /{type}` (standalone re-upload,
separate from onboarding)

**Account Settings** (`/api/delivery/account-settings`): `GET /`, `PUT /`

**Orders** (`/api/delivery/orders`): `GET /` (active/completed, paginated), `GET /{id}`
(full detail: timeline + contacts + proof), `GET /nearby`, `PUT /{id}/accept`,
`PUT /{id}/reject`, `PUT /{id}/arrived-at-store`, `PUT /{id}/pickup`,
`PUT /{id}/arrived-at-drop`, `POST /{id}/proof-of-delivery` (multipart), `POST /{id}/verify-otp`,
`PUT /{id}/complete`
> ⚠️ Missing under new design: pickup-OTP verification step (section 5 above). Accept
> logic also needs the atomic-conditional-accept fix (section 3).

**Earnings** (`/api/delivery/earnings`): `GET /` (period, with fare breakdown),
`GET /trend`

**Test-only** (`/api/delivery/test`, delete before prod): `POST /approve-kyc`,
`POST /dispatch`

---

## Vendor module — full endpoint inventory (as built, pre-pivot)

**Onboarding** (`/api/vendor/onboarding`): `GET /status`, `PUT /basic-info`,
`PUT /business-location`, `POST /submit`, `GET /checklist`

**Documents** (`/api/vendor/documents`): `GET /`, `POST /{type}` (BUSINESS_LICENSE, TAX_ID,
GOVERNMENT_ID)

**Test-only** (`/api/vendor/test`, delete before prod): `POST /approve-kyc`,
`POST /reject-kyc`

**Store Profile** (`/api/vendor/store-profile`): `GET /`, `PUT /` (name, bio, image,
attribute tags, pickup address)

**Account Settings** (`/api/vendor/account-settings`): `GET /`, `PUT /` (fullName, email,
phone, business license number, profile image, 3 notification toggles)

**Operating Hours** (`/api/vendor/operating-hours`): `GET /`, `PUT /` (bulk, all 7 days),
`POST /closures`, `DELETE /closures/{id}`. Auto-creates default schedule on first fetch.
⚠️ Not enforced anywhere — `browseNearbyShops`/order-accept only check
`isOnline`+`kycStatus`, not hours/closures.

**Orders** (`/api/vendor/orders`): `GET /{id}` enriched detail (items filtered to this
shop's own products, subtotal/serviceFee[flat 10% placeholder]/total, customerPhone
resolved live). `@PreAuthorize` present here (fixed). Rest of `VendorOrderController`
(list, accept, reject, status update) — **still needs `@PreAuthorize` audit** and, per
the pivot, needs the atomic-accept rework + "ready for pickup" dispatch call.

**Profile Hub** (`/api/vendor/profile/stats`): totalSales, storeRating (nullable),
ratingCount, activeItemsCount, isVerified.

> Pre-pivot product/inventory/category endpoints (`VendorProductController`,
> `VendorInventoryController`, `VendorCategoryController`) still exist as originally
> built (vendor sets own name/price/description) — **these are exactly what needs
> reworking first** once Admin's catalog exists, per the rework section above.

---

## Cross-module interfaces — the real map of who calls whom

| Interface | Owned by | Status |
|---|---|---|
| `UserLookupService` | Auth | ✅ Real, used live by Delivery for phone/identity resolution |
| `ProductCatalogService` | Vendor | Real implementation seen, currently backed by vendor-owned `Product` — **will need to be re-pointed at Admin's catalog** under the new model. Has `getAllCategories`, `getRelatedProducts`, `getDailyDeals`, `browseNearbyShops`, `searchProducts`. |
| `CustomerOrderService` | Customer | ✅ Confirmed **real**, not a stub — `acceptOrder`/`rejectOrder`/`updateOrderStatus` genuinely transition `Order` via `OrderService`. Also has `getOrdersByShopId` (real JPQL join through `OrderItem→Product→shop.id` — **will need rework once products aren't vendor-owned**), and three methods not previously known about: `assignDeliveryAgent(...)`, `markDelivered(...)`, `getDeliveryOtp(...)`. |
| `DeliveryDispatchService` | Delivery | Real, callable, but **nothing calls it yet in real Vendor code** (only the test-only dispatch endpoint exercises it). This is the method Vendor's "ready for pickup" action needs to call, per the pivot. |
| `DeliveryRatingService` | Delivery | Real, but nothing calls it yet — `Customer.rateOrder` should call this. No HTTP endpoint exists (Java-interface only). |
| `VendorRatingService` | Vendor | Real (`rateShop(orderId, shopId, ...)`), but nothing calls it yet — same situation as above. No HTTP endpoint exists. |

### Newly discovered Customer methods — decisions made about them

- **`assignDeliveryAgent(orderId, agentName, agentPhone, agentPhotoUrl, estimatedWindow)`**
  — exists, currently never called. Delivery's `acceptAssignment()` **should** call this
  the moment a partner accepts, so `Customer.trackOrder()` shows the real courier instead
  of its current hardcoded fallback ("John Veggie"). **Not yet wired.**
- **`markDelivered(orderId, deliveryPhotoUrl, locationNote)`** — exists, currently never
  called; Delivery's `completeDelivery()` currently calls the generic
  `updateOrderStatus(orderId, "DELIVERED")` instead, which loses the proof-of-delivery
  photo that Delivery already captures (`DeliveryProofOfDelivery`). **Should be wired to
  call this instead — not yet done.**
- **`getDeliveryOtp(orderId)`** — exists, but implementation is weak
  (`Math.abs(orderId.hashCode() % 9000) + 1000` — deterministic from order ID alone, no
  expiry, no attempt limit). **Decision: do not switch to this.** Keep Delivery's own OTP
  system (random, time-limited, attempt-capped, per-assignment) as the real one; treat
  this Customer-side method as legacy/dead code. Flagged explicitly so nobody
  "simplifies" toward the weaker version later.

---

## Known bugs (confirmed, not yet fixed)

- **Promo-code bug**: `CartServiceImpl` correctly tracks and recomputes `promoCode`/
  `promoDiscount` on the cart, but `OrderServiceImpl.checkout()` hardcodes
  `promoDiscount = BigDecimal.ZERO` (comment admits "for simplicity... let's mock a
  promo") instead of reading the cart's real value. Same bug repeats in
  `getCheckoutSummary()`. Customer sees a discount in-cart, gets charged full price at
  checkout. **Independent of the architecture pivot — a real, standalone bug.**
- **Race condition in `DeliveryAssignmentServiceImpl.acceptAssignment()`**: no atomic
  guard between reading and writing assignment status; `@Version` exists on
  `BaseEntity` but the resulting `OptimisticLockException` isn't caught/handled anywhere
  — would surface as a raw 500 instead of a clean conflict response. **Needs fixing as
  part of the broadcast rework (section 3), but is a pre-existing bug in already-shipped
  code, not new scope.**
- **Vendor `fullName` captured at registration, never persisted** (pre-existing, still
  unfixed).
- Duplicate class names: two unrelated `ProductDto` and two unrelated `ShopDto` across
  Vendor sub-packages (pre-existing, still unfixed, and will need real reconciliation
  once catalog ProductDto shape changes anyway).

---

## Known gaps flagged along the way (not yet fixed, by design — future work)

- Auth's `User` entity has **no name field at all** — Delivery worked around this with a
  local `fullName` on `DeliveryPartnerProfile`; Vendor has the same workaround on its own
  account-settings fields. Same likely gap affects Customer's display-name needs
  (`CustomerProfile.fullName` is separately stored there too — three modules now each
  have their own local copy of "the user's name" instead of one canonical source. Worth
  a real fix in Auth eventually, not urgent).
- `V6`, `V7`, `V8` migrations contain Auth/Customer content sitting inside Platform's
  reserved `V1–V19` range instead of their own ranges — pre-existing, unresolved, not
  caused by any module built in this chat.
- `CustomerOrderService.updateOrderStatus` takes a raw `String`, not the `OrderStatus`
  enum — pending decision, not yet changed.
- Bank details (Step 3 of Delivery onboarding) are stored in Delivery module as a
  short-term exception — flagged for eventual migration to Payment module. Vendor's
  Payout Settings are stubbed the same way.
- Vendor has no dedicated `ShopLookupService` — Delivery snapshots shop name/address at
  dispatch time instead of resolving live.
- `DeliveryPartnerRating`/`VendorShopRating` have no test-only HTTP endpoint yet
  (decision pending for both).
- Vendor: no `@PreAuthorize` on Product/Inventory/Dashboard/Report/Category/Status
  controllers (Order controller fixed).
- `VendorDashboardService`/`VendorReportService` still directly import Customer's
  `Order` entity — unfixed cross-module violation (not repeated in newer Vendor code,
  which uses `CustomerOrderService.getOrdersByShopId` + local filtering instead).
- Operating hours/closures not enforced in browse or order-accept paths.
- Vendor Payout Settings / Request Payout screens — deferred like Delivery's withdrawals
  (unlike Wallet, which is now required real scope — see architecture section).

---

## Vendor module — audit findings (original, pre-Figma-build; still relevant, several
## items already fixed as noted)

**Critical — cross-module boundary violations:**
- ~~`VendorAuthServiceImpl` directly imports Auth's `User`/`UserRole`/`RefreshToken`
  entities...~~ **FIXED** — password auth retired entirely, Vendor now uses shared OTP
  endpoints (`role: VENDOR`), same as Delivery.
- `VendorDashboardService` / `VendorReportService` directly import Customer's
  `Order`/`OrderStatus` and inject Customer's `OrderRepository` — **still unfixed**,
  should go through `CustomerOrderService` instead (newer Vendor code does this
  correctly by contrast).

**Security gaps:**
- No `@PreAuthorize` on most Vendor controllers — **partially fixed** (Order controller
  done); Product/Inventory/Dashboard/Report/Category/Status controllers still open.
- ~~`ShopRegistrationRequestDto.ownerUserId` client-suppliable~~ — superseded; onboarding
  flow now uses `SecurityUtils.getCurrentUserId()` throughout.
- ~~`VendorShopService.submitKycDocuments()` auto-approves KYC synchronously in the real
  endpoint~~ **FIXED** — real submission now requires documents uploaded first; auto-
  approve isolated to test-only controller.

**Bugs:**
- `fullName` captured at vendor registration, never persisted — **still unfixed**.
- Duplicate class names (`ProductDto`, `ShopDto`) — **still unfixed**.

**Gaps vs. original doc's Vendor spec:**
- No inventory deduction on order accept — **now superseded**; under the new model,
  quantity-based deduction isn't the mechanism at all (`isListed` toggle instead,
  confirmed at accept-time).
- No `DeliveryDispatchService` call on order accept — **still open**, and per the pivot
  now needs to fire on "ready for pickup" instead of "accept."
- `getDailyDeals()`/`getRelatedProducts()` don't filter by shop online/KYC status while
  `browseNearbyShops()`/`searchProducts()` do — **still unfixed**, and will need
  rethinking anyway once these read from Admin's catalog.

**Structural duplication:**
- Two parallel onboarding flows — **resolved**; real onboarding now lives under
  `/api/vendor/onboarding` + `/api/vendor/documents`, old `VendorShopController`
  KYC-submit path retired as part of the onboarding-phase build.

---

## Postman collections delivered so far

- Delivery: cumulative collection covering onboarding → documents → account settings →
  KYC test-approve → status/discovery → full order lifecycle (accept → arrived-at-store
  → pickup → arrived-at-drop → proof-of-delivery → verify-otp → complete) → earnings.
  **Will need the pickup-OTP step inserted once that's built.**
- Vendor: cumulative collection covering onboarding (9 steps) → test-approve/reject →
  store profile/account settings/stats → shop & status → categories → products &
  inventory → orders → dashboard & reports. **Will need rework once product
  create/list endpoints change to catalog-select/toggle-isListed.**

---

## Merge-conflict / workflow rules (confirmed with user, apply to every module)

1. **Own package, own Flyway migration range, no direct `@Entity` imports across
   modules.** Cross-module reads/writes go only through a `@Service` interface the
   owning module exposes.
2. **If a module needs something from another module that doesn't exist yet as a clean
   interface method**, the owning module's interface gets extended (documented exactly
   why), rather than reaching into its tables directly. This is not a boundary violation
   — it's the intended mechanism. A boundary violation is importing the other module's
   `@Entity`/`Repository` directly, which has happened twice so far (Vendor→Auth, fixed;
   Vendor→Customer in Dashboard/Report, still open) and gets flagged as a bug when found.
3. **Every zip delivered is the complete, self-contained module** — original code + all
   changes, fully merged, ready to drop `src/` straight over the existing package with
   zero manual merging required. Not a diff, not a delete-list. Applies to every module
   from here on (established after an early Vendor round where a delete-list was used
   once and explicitly corrected).
4. **Test-only endpoints** (KYC auto-approve, dispatch triggers, etc.) live in a
   clearly-named `...TestController`, under a `/test` sub-path, and are called out
   explicitly as "delete before production" in that module's `NOTES_*.md`. They exist
   only to unblock manual testing where a real upstream module (usually Admin) doesn't
   exist yet.
5. **Every module's real gaps and deferred decisions get written into that module's own
   `NOTES_<MODULE>.md`**, and the high-level cross-module version of the same gaps gets
   folded into this shared `PROJECT_STATE.md` — so a gap is always visible from at least
   one of two places, never only living in chat history.
6. **A genuine architecture change (like the catalog/cart pivot) gets fully talked
   through and explicitly locked, point by point, before any code is written against
   it** — confirmed answers get restated back before being treated as final, and a
   rejected alternative gets documented (see "Explicitly dropped/rejected alternatives"
   above) so it isn't accidentally re-proposed later without context.
7. **Race conditions / concurrent-write risks get resolved at the database layer**
   (atomic conditional updates, relying on `@Version` optimistic locking + catching the
   resulting conflict), never via notification timing/staggering — staggering was
   proposed once for exactly this reason and explicitly rejected.
8. **Building/checking order for a new module**: audit existing code first if any
   exists → flag violations/bugs/gaps found → get explicit decisions on anything
   ambiguous → build in scoped phases → cumulative zip + updated `NOTES_*.md` after each
   phase → cumulative Postman collection kept in sync → update this file after any
   architecture-level decision or module-completion milestone.

---

## Next steps

1. **Build Admin module** — nothing in the new architecture works without it. Needs at
   minimum: master product catalog (CRUD + image + category/subcategory + fixed price),
   vendor product-request review/approval queue, KYC approval for Vendor/Delivery (real,
   replacing the test-only stubs in both modules), radius-configuration (customer-facing
   search/broadcast radius, dynamic/admin-set), platform commission percentage
   (replaces Vendor's flat-10% placeholder), vendor-accept / delivery-accept **per-round**
   timeout configuration (capped at a sane hard max in code, per the locked Payment
   design), and the **separate** re-broadcast bound configuration for the cancel/retry
   loop (section 8) — max rounds AND max total elapsed time, both admin-set,
   independent of the per-round timeouts above.
2. **Build Payment module** — design is fully locked (see architecture section 6):
   Razorpay integration (Orders API, manual capture, capture/void), Wallet ledger for
   Customer/Vendor/Delivery, sequential multi-order checkout allocation, webhook
   handling for async payment events. Bank withdrawal/Razorpay Route payout can stay
   stubbed initially. Likely needs to exist before or alongside Customer's cart/checkout
   rework, since checkout now depends on it directly.
3. Once Admin's catalog is real, **rework Vendor's product/inventory endpoints** to
   select-from-catalog + `isListed` toggle instead of vendor-created products.
4. **Rebuild Customer's cart** for the multi-cart model, and checkout for
   one-payment-with-wallet-checkbox → N-orders, each with its own
   wallet-reservation + Razorpay-hold split per the locked Payment design.
5. **Fix the atomic-accept race condition** in Delivery (existing bug) while building
   the same pattern fresh for Vendor's new broadcast-accept flow. Accept/reject
   timeouts on both sides now come from Admin config, not hardcoded values.
6. **Add pickup-OTP** to Delivery (vendor-issues, partner-enters).
7. **Wire the three real `CustomerOrderService` methods** (`assignDeliveryAgent`,
   `markDelivered`, `getDeliveryOtp`-is-legacy-decision) into Delivery's assignment
   lifecycle.
8. Housekeeping bugs that are independent of the pivot and can be fixed whenever
   convenient: promo-code-not-applied-at-checkout, Vendor `fullName` not persisted,
   duplicate `ProductDto`/`ShopDto` class names, missing `@PreAuthorize` on remaining
   Vendor controllers.
