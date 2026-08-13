-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V47
-- Payment Module: Payments table + Order payment fields
-- ============================================================
-- payments table: one row per Razorpay payment attempt.
-- An order can have multiple attempts (e.g. customer retries
-- after a failure), but only one CAPTURED row per order.
--
-- Also adds razorpay_order_id + payment_status to the orders
-- table so the frontend can read payment context without a join.
-- ============================================================

CREATE TABLE payments (
    id                      UUID            NOT NULL PRIMARY KEY,
    created_at              TIMESTAMP(6)    NOT NULL,
    updated_at              TIMESTAMP(6)    NOT NULL,
    deleted_at              TIMESTAMP(6)    NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,

    -- VegGoFresh order reference
    order_id                UUID            NOT NULL,
    user_id                 UUID            NOT NULL,

    -- Razorpay identifiers
    razorpay_order_id       VARCHAR(100)    NOT NULL UNIQUE, -- order_XXXX from Razorpay
    razorpay_payment_id     VARCHAR(100)    NULL,            -- pay_XXXX after capture
    razorpay_signature      VARCHAR(500)    NULL,            -- stored for audit trail

    -- Financial
    amount                  DECIMAL(12,2)   NOT NULL,        -- in INR (not paise)
    currency                VARCHAR(10)     NOT NULL DEFAULT 'INR',

    -- Status: CREATED | CAPTURED | FAILED | REFUNDED
    status                  VARCHAR(30)     NOT NULL,

    -- Failure metadata
    failure_reason          VARCHAR(500)    NULL,

    -- Last webhook event type processed (for idempotency audit)
    webhook_event           VARCHAR(100)    NULL
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_rzp_order_id ON payments (razorpay_order_id);
CREATE INDEX idx_payments_status ON payments (status);

-- Add Razorpay payment context columns to orders table.
-- razorpay_order_id: set when payment order is created (before payment).
-- payment_status: mirrors Payment.status for quick read without join.
ALTER TABLE orders
    ADD COLUMN razorpay_order_id VARCHAR(100) NULL,
    ADD COLUMN payment_status    VARCHAR(30)  NULL;

-- Existing orders (before payment module) get a synthetic status
-- so the NOT NULL constraint doesn't break them. NULL means legacy/COD.
-- payment_status NULL = pre-payment-module order (treat as paid/COD).
