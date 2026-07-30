-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V97: Delivery Assignment Status History Table
--
-- One row per status transition. Powers the delivery-detail timeline
-- (Order Accepted -> Arrived at Store -> Order Picked Up -> Delivered).
-- Rows are insert-only, created_at IS the event timestamp.
-- ============================================================

CREATE TABLE delivery_assignment_status_history (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    assignment_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_status_history_assignment FOREIGN KEY (assignment_id) REFERENCES delivery_assignments(id)
);

CREATE INDEX idx_delivery_status_history_assignment ON delivery_assignment_status_history(assignment_id);
