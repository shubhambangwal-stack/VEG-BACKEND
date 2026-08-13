-- ═══════════════════════════════════════════════════════════════════════
-- Drops the leftover single-cart-per-user constraint from before the
-- multi-cart rework. The Cart entity no longer has unique = true on
-- user_id (customers can now hold multiple carts, one per vendor), but
-- that Java-side change never removed the actual DB constraint that a
-- prior migration created. Confirmed present via:
--   SELECT conname, contype FROM pg_constraint WHERE conrelid = 'carts'::regclass;
-- -> uk_carts_user | u
-- ═══════════════════════════════════════════════════════════════════════

ALTER TABLE carts DROP CONSTRAINT IF EXISTS uk_carts_user;