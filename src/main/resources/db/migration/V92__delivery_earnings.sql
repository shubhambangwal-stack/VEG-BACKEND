-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V92: Delivery Earnings Table
-- ============================================================

CREATE TABLE delivery_earnings (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    delivery_partner_user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_earnings_order UNIQUE (order_id),
    CONSTRAINT fk_delivery_earnings_partner FOREIGN KEY (delivery_partner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_delivery_earnings_partner ON delivery_earnings(delivery_partner_user_id);
