-- ═══════════════════════════════════════════════════════════════════════
-- Admin Module: Replace platform_commission_percent (%) with two flat
-- rupee amounts: platform_fee_amount and delivery_fee_amount.
-- Both are stored as NUMERIC(10,2) (rupees, not percentages).
-- Default values match what was previously hardcoded in the codebase:
--   platform_fee_amount  = 5.00  (was BigDecimal.valueOf(5.00))
--   delivery_fee_amount  = 20.00 (was BigDecimal.valueOf(20.00))
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE platform_settings
    DROP COLUMN platform_commission_percent;

ALTER TABLE platform_settings
    ADD COLUMN platform_fee_amount  NUMERIC(10,2) NOT NULL DEFAULT 5.00,
    ADD COLUMN delivery_fee_amount  NUMERIC(10,2) NOT NULL DEFAULT 20.00;
