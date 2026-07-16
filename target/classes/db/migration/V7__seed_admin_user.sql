-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V7
-- Auth Module: Seed Admin User
-- ============================================================

INSERT INTO users (id, created_at, updated_at, deleted_at, version, phone, email, password, role, is_verified, is_blocked)
VALUES (
    'e837cfbe-7d6f-474c-8bb3-455b55018b10',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL,
    0,
    '+910000000000',
    'admin@veg.go',
    '$2a$12$0xtehnDIDPxxlEDcdsvi7uznca3VImlRmHHi7NtD7K0l9hMlT0hqW',
    'ADMIN',
    true,
    false
);
