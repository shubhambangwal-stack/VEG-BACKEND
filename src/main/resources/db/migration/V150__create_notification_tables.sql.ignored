-- ============================================================
-- VegGo Fresh ù Notification Module (V150 range)
-- V150: Notifications Table
-- ============================================================

CREATE TABLE IF NOT EXISTS notifications (
    id               UUID NOT NULL,
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at       TIMESTAMP(6) NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    recipient_type   VARCHAR(50) NOT NULL,
    recipient_id     UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title            VARCHAR(200) NOT NULL,
    message          TEXT NOT NULL,
    payload          TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at          TIMESTAMP(6),
    read_at          TIMESTAMP(6),
    action_url       VARCHAR(500),
    priority         VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    delivery_channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    expires_at       TIMESTAMP(6),
    PRIMARY KEY (id)
);

-- Soft delete index
CREATE INDEX idx_notifications_deleted_at ON notifications (deleted_at);

-- Common query indexes
CREATE INDEX idx_notifications_recipient ON notifications (recipient_type, recipient_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_type ON notifications (notification_type);
CREATE INDEX idx_notifications_expires ON notifications (expires_at);
