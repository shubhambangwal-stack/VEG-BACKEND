-- ============================================================
-- VegGo Fresh — Delivery Module (V90-V109 range)
-- V94: Account Settings fields on delivery_partners
--
-- full_name is a WORKAROUND -- users.* has no name column, and
-- UserSummaryDto exposes only phone/email/role/verified/blocked. Ideally
-- this lives on Auth's User entity. See NOTES.md.
-- ============================================================

ALTER TABLE delivery_partners
    ADD COLUMN full_name VARCHAR(255) NULL,
    ADD COLUMN email VARCHAR(255) NULL,
    ADD COLUMN vehicle_color VARCHAR(50) NULL,
    ADD COLUMN push_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN sms_alerts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_newsletters_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN emergency_contact_name VARCHAR(255) NULL,
    ADD COLUMN emergency_contact_relationship VARCHAR(100) NULL,
    ADD COLUMN emergency_contact_phone VARCHAR(20) NULL;
