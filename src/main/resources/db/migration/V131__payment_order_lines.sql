-- ═══════════════════════════════════════════════════════════════════════
-- One row per Customer Order created out of a checkout() call, always
-- belonging to exactly one payment_orders batch (an Order is created once,
-- by exactly one checkout() call -- see OrderServiceImpl.checkout()).
-- order_id is a loose UUID reference only, same convention as everywhere
-- else cross-module (WalletTransaction.reference_id, DeliveryAssignment
-- .order_id, etc.) -- no FK to Customer's orders table, no cross-module
-- JPA relation.
--
-- status drives the capture decision: while any line for a payment_orders
-- batch is still PENDING, no capture happens. Once every line has resolved
-- to ACCEPTED or VOIDED, PaymentServiceImpl issues exactly one Razorpay
-- capture call for the sum of the ACCEPTED lines' amounts (partial capture
-- if some lines were VOIDED) -- Razorpay only allows a single capture per
-- payment, so this fan-in has to happen exactly once, not per line.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE payment_order_lines (
    id                UUID PRIMARY KEY,
    payment_order_id  UUID NOT NULL,
    order_id          UUID NOT NULL,
    amount            NUMERIC(12,2) NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    resolved_at       TIMESTAMP,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP,
    deleted_at        TIMESTAMP,
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_payment_order_lines_order_id ON payment_order_lines(order_id);
CREATE INDEX idx_payment_order_lines_payment_order_id ON payment_order_lines(payment_order_id);
