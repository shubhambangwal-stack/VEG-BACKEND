-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V100: Fare breakdown columns on delivery_earnings
--
-- 'amount' stays as the running total (backward compatible with anything
-- already summing on it). peak_bonus and tip default to 0 and stay 0 in
-- code for now -- no surge/demand system and no tip-collection mechanism
-- exist anywhere yet. See NOTES.md.
-- ============================================================

ALTER TABLE delivery_earnings
    ADD COLUMN base_pay DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN distance_fare DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN peak_bonus DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN tip DECIMAL(12,2) NOT NULL DEFAULT 0;
