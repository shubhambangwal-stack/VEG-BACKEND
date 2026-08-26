-- ═══════════════════════════════════════════════════════════════════════
-- Wallet Top-up feature: add is_topup flag to payment_orders table
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE payment_orders ADD COLUMN is_topup BOOLEAN NOT NULL DEFAULT false;
