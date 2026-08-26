-- ═══════════════════════════════════════════════════════════════════════
-- Add version column to Payment module tables that extend BaseEntity
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE payment_orders ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_order_lines ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_webhook_events ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payout_requests ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
