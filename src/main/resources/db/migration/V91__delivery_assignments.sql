-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V91: Delivery Assignments Table
-- ============================================================

CREATE TABLE delivery_assignments (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    order_id UUID NOT NULL,
    delivery_partner_user_id UUID NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    drop_latitude DOUBLE PRECISION NOT NULL,
    drop_longitude DOUBLE PRECISION NOT NULL,
    assigned_at TIMESTAMP(6),
    expires_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_assignments_partner FOREIGN KEY (delivery_partner_user_id) REFERENCES users(id)
);

CREATE INDEX idx_delivery_assignments_order_id ON delivery_assignments(order_id);
CREATE INDEX idx_delivery_assignments_status ON delivery_assignments(status);
CREATE INDEX idx_delivery_assignments_partner ON delivery_assignments(delivery_partner_user_id);
