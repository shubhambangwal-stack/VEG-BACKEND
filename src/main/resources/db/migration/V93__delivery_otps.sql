-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V93: Delivery Completion OTPs Table
--
-- Local workaround: CustomerOrderService.getDeliveryOtp(orderId) does not
-- exist yet on the Customer module's stub interface. Delivery generates and
-- verifies its own completion OTP here instead of blocking on that method.
-- ============================================================

CREATE TABLE delivery_otps (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    assignment_id UUID NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_otps_assignment UNIQUE (assignment_id),
    CONSTRAINT fk_delivery_otps_assignment FOREIGN KEY (assignment_id) REFERENCES delivery_assignments(id)
);
