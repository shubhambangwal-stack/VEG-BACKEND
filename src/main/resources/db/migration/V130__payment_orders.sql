-- ═══════════════════════════════════════════════════════════════════════
-- Payment module, Phase 2.1 (foundation). Reserved migration range for
-- Payment is V130-V149 (confirmed against real migrations: top out at
-- V121, no V13x has been used by any other module).
--
-- payment_orders = ONE Razorpay order per checkout call, i.e. per PAYMENT,
-- not per Order. OrderRequestDto/OrderService.checkout()'s own docs are
-- explicit: "one payment can fan out into N independent orders" -- a
-- customer with items from 2 vendor-only carts pays ONCE for the combined
-- total, and that single Razorpay order/payment then covers however many
-- Order rows checkout() created. The per-Order breakdown lives in
-- payment_order_lines (V131), not here.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE payment_orders (
    id                    UUID PRIMARY KEY,
    user_id               UUID NOT NULL,
    razorpay_order_id     VARCHAR(64) NOT NULL,
    razorpay_payment_id   VARCHAR(64),
    currency              VARCHAR(3) NOT NULL DEFAULT 'INR',
    total_amount          NUMERIC(12,2) NOT NULL,
    captured_amount       NUMERIC(12,2),
    status                VARCHAR(30) NOT NULL,
    authorized_at         TIMESTAMP,
    captured_at           TIMESTAMP,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP,
    deleted_at            TIMESTAMP,
    version               BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_payment_orders_razorpay_order_id ON payment_orders(razorpay_order_id);
CREATE INDEX idx_payment_orders_user_id ON payment_orders(user_id, created_at DESC);
