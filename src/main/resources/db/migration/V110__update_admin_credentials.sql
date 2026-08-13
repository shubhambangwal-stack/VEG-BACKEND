-- ============================================================
-- VegGo Fresh Platform — Flyway Migration V110
-- Admin Module: Update seeded admin credentials to live values
-- ============================================================
-- This updates the single bootstrap admin account created in V7
-- (Auth's range) with the real login credentials for this
-- environment. See NOTES_ADMIN.md → "Admin account creation
-- story" for why this is the only mechanism for admin accounts
-- right now (single hardcoded seed, no self-registration/invite
-- flow exists yet).

UPDATE users
SET email      = 'aakash@gmail.com',
    password   = '$2b$12$FgQ5KzX6uFcym.fLd4sy4eergx4B/tcUme9CbDFQucfbIUig78ysu', -- bcrypt(#admin), cost 12
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = 'e837cfbe-7d6f-474c-8bb3-455b55018b10'
  AND role = 'ADMIN';