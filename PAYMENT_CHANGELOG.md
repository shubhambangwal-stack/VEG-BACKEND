# VegGo Fresh - Payment Phase 2 & 3 Implementation

This document outlines all the new APIs, database tables, and system changes introduced during the Payment Module Phase 2 (Hold/Capture) and Phase 3 (Payouts & Ledger) development.

## 1. Database Migrations & New Tables
We successfully introduced several new tables to support the payment lifecycle without disrupting existing Customer/Delivery modules.

* **`payment_orders`** (`V130`): Tracks the main Razorpay Order created during checkout. Represents a single payment hold for a combined cart checkout.
* **`payment_order_lines`** (`V131`): Maps the `payment_orders` back to individual customer `orders`. Required because a single checkout (and single payment) can fan out into multiple vendor orders.
* **`payment_webhook_events`** (`V132`): Idempotency ledger for incoming Razorpay webhooks. Ensures that if Razorpay sends the same event twice, it is only processed once.
* **`payout_requests`** (`V133`): Queue for Vendors and Delivery Partners requesting bank transfers. 
* **`wallet_transactions`**: Existing table, but introduced new `WalletTransactionReason` Enums (`PAYOUT_DEBIT`, `ORDER_CANCELLED_REFUND`, `ORDER_CANCELLED_POST_CAPTURE_REFUND`, `WALLET_TOP_UP`).
* **BaseEntity Versioning** (`V136`): Added `version` column to all new payment tables for Hibernate Optimistic Locking.

---

## 2. New APIs Created

### A. Razorpay Webhook Integration (System/Automated)
Used by Razorpay to notify the backend about payment successes or failures.
* **`POST /api/v1/payment/webhook`**
  * **Headers**: `X-Razorpay-Signature` (Required for security validation)
  * **Payload**: Razorpay Event JSON (e.g., `payment.authorized`, `payment.captured`)
  * **Logic**: Verifies signature using `razorpay.webhookSecret`, checks idempotency in `payment_webhook_events`, and triggers downstream capture/void logic.

### B. Admin Payout APIs (Manual Bank Transfer Approvals)
Since Razorpay Route KYC is pending, payouts are currently managed manually via an Admin queue.

* **`GET /api/v1/admin/payment/payout-requests`**
  * **Query Params**: `status` (PENDING, COMPLETED, REJECTED), `page`, `size`
  * **Description**: Lists all withdrawal requests from Vendors and Delivery Partners.

* **`POST /api/v1/admin/payment/payout-requests/{id}/approve`**
  * **Payload**: `{"adminNotes": "Transferred via NEFT Ref: 12345"}`
  * **Description**: Marks the request as `COMPLETED`. Since the funds were already held/locked during the request creation, this simply finalizes the ledger.

* **`POST /api/v1/admin/payment/payout-requests/{id}/reject`**
  * **Payload**: `{"adminNotes": "Bank account details incorrect"}`
  * **Description**: Marks the request as `REJECTED` and safely **refunds** the locked amount back into the Vendor's/Driver's platform wallet.

---

## 3. Internal Service Integrations (Payment Lifecycle)

These are internal Spring Boot service hooks connecting the Payment module to existing modules.

* **Payment Capture on Delivery**: When an order is delivered (`DeliveryAssignmentServiceImpl.markDelivered`), the payment module is notified to transition the hold to `CAPTURED`. If all orders in a batch are delivered, Razorpay capture is executed.
* **Payment Void on Cancellation**: When a customer cancels or a timeout occurs (`OrderServiceImpl`, `VendorAcceptTimeoutSweepService`), the order amount is cleanly refunded to the user's wallet via `WalletService.credit()`.

## 4. Postman Collections Added
* **`VegGoFresh_Complete_Testing_Collection.json`**: Available in the project root. It contains the exact step-by-step requests required to simulate:
  1. Creating a Customer & Vendor
  2. Adding items to cart & Checkout
  3. Simulating Razorpay Webhook `payment.authorized`
  4. Vendor Accepting Order
  5. Delivery marking as Delivered (Triggers Razorpay Capture)
  6. Admin Payout Approval

## 5. Configuration Updates (`application.yml`)
New keys required for the payment system (values can be mock data for local testing):
```yaml
razorpay:
  keyId: rzp_test_xxxxx
  keySecret: your_secret
  webhookSecret: your_webhook_secret
payment:
  payoutsEnabled: false # Set to true ONLY when Razorpay KYC is fully complete
```
