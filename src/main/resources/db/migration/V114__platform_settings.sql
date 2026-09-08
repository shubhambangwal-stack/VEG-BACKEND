-- ═══════════════════════════════════════════════════════════════════════
-- Platform-wide configuration (single row). Rename this file to the next
-- available V-number in Admin's V110-V129 range before applying.
-- ═══════════════════════════════════════════════════════════════════════

CREATE TABLE platform_settings (
    id                              UUID PRIMARY KEY,
    delivery_radius_km              DOUBLE PRECISION NOT NULL DEFAULT 10.0,
    platform_commission_percent     NUMERIC(5,2) NOT NULL DEFAULT 10.00,
    vendor_accept_timeout_seconds   INTEGER NOT NULL DEFAULT 300,
    delivery_accept_timeout_seconds INTEGER NOT NULL DEFAULT 60,
    rebroadcast_max_rounds          INTEGER NOT NULL DEFAULT 5,
    rebroadcast_max_elapsed_minutes INTEGER NOT NULL DEFAULT 30,
    created_at                      TIMESTAMP NOT NULL,
    updated_at                      TIMESTAMP,
    deleted_at                      TIMESTAMP
);

-- Deliberately NOT seeding a row here -- PlatformSettingsServiceImpl.getOrCreateSettings()
-- auto-creates one with entity defaults on first access, same pattern as
-- CustomerProfile. A pre-seeded row here would just be redundant.
