-- ═══════════════════════════════════════════════════════════════════════
-- Payout requests: vendor and delivery partner withdrawal request queue.
-- Admin approves/rejects; actual bank transfer is manual until Razorpay
-- Route KYC is complete (payoutsEnabled=false).
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE payout_requests (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    amount              NUMERIC(12,2) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    razorpay_payout_id  VARCHAR(64),
    admin_notes         VARCHAR(500),
    processed_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,
    deleted_at          TIMESTAMP
);

CREATE INDEX idx_payout_requests_user_id ON payout_requests(user_id);
CREATE INDEX idx_payout_requests_status ON payout_requests(status);
