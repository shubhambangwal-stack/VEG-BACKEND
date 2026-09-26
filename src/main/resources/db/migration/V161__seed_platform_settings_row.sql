-- Seed the platform_settings singleton row.
--
-- WHY: V114 deliberately left this table empty and relied on
-- PlatformSettingsServiceImpl.getOrCreateSettings() to INSERT a row "on first
-- access". That put a WRITE (saveAndFlush) inside methods declared
-- @Transactional(readOnly = true), which are called from inside other services'
-- write transactions -- most damagingly CartServiceImpl.addItemToCart(), which
-- reads the delivery/platform fees once per cart while mapping the response.
--
-- Two concrete failures that caused:
--   1. saveAndFlush() forces an immediate flush of the ENTIRE persistence
--      context, so addItemToCart's still-pending Cart/CartItem inserts were
--      flushed mid-business-logic, from inside a readOnly-declared call.
--   2. On an unseeded table, concurrent requests both saw zero rows and both
--      tried to INSERT the singleton. The loser's constraint/lock failure
--      escaped a nested @Transactional method, which makes Spring mark the
--      shared transaction rollback-only. The caller swallowed it and returned
--      "successfully", so the failure only surfaced at commit as:
--      UnexpectedRollbackException: Transaction silently rolled back because it
--      has been marked as rollback-only
--
-- The fix is to guarantee exactly one row exists before the app serves traffic,
-- so the read path can be a pure SELECT. Guarded so it stays idempotent and safe
-- against databases where a row was already auto-created by the old behaviour.

INSERT INTO platform_settings (
    id,
    delivery_radius_km,
    platform_fee_amount,
    delivery_fee_amount,
    vendor_accept_timeout_seconds,
    delivery_accept_timeout_seconds,
    rebroadcast_max_rounds,
    rebroadcast_max_elapsed_minutes,
    otp_expiry_minutes,
    version,
    created_at,
    updated_at
)
SELECT
    'a1000000-0000-0000-0000-000000000001'::uuid,
    10.0,
    5.00,
    20.00,
    300,
    60,
    5,
    30,
    120,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM platform_settings);
