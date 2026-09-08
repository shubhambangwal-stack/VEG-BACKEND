-- ═══════════════════════════════════════════════════════════════════════
-- User Bank Accounts & Payout Request extensions for RazorpayX Payouts.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE user_bank_accounts (
    id                      UUID PRIMARY KEY,
    user_id                 UUID NOT NULL UNIQUE,
    account_holder_name     VARCHAR(100) NOT NULL,
    account_number          VARCHAR(50) NOT NULL,
    ifsc_code               VARCHAR(20) NOT NULL,
    bank_name               VARCHAR(100),
    upi_id                  VARCHAR(100),
    razorpay_contact_id     VARCHAR(64),
    razorpay_fund_account_id VARCHAR(64),
    is_verified             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_bank_accounts_user_id ON user_bank_accounts(user_id);

-- Extend existing payout_requests table with user_role, bank_account_id, failure_reason, and version
ALTER TABLE payout_requests ADD COLUMN IF NOT EXISTS user_role VARCHAR(20) NOT NULL DEFAULT 'VENDOR';
ALTER TABLE payout_requests ADD COLUMN IF NOT EXISTS bank_account_id UUID;
ALTER TABLE payout_requests ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);
ALTER TABLE payout_requests ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
