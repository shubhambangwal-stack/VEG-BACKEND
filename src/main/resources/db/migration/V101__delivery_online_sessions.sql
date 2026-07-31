-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V101: Delivery Online Sessions Table
--
-- One row per online/offline toggle. Powers Active Hours / Work Hours shown
-- on Earnings, Weekly Performance, and Profile Hub screens. ended_at NULL
-- means the session is still open.
-- ============================================================

CREATE TABLE delivery_online_sessions (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    delivery_partner_user_id UUID NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    ended_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_online_sessions_partner FOREIGN KEY (delivery_partner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_delivery_online_sessions_partner ON delivery_online_sessions(delivery_partner_user_id);
