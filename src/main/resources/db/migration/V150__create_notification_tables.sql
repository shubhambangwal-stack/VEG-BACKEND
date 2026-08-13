-- V150__create_notification_tables.sql
-- Notification module migration for VegGo Fresh

CREATE TABLE IF NOT EXISTS notifications (
    id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    recipient_type VARCHAR(50) NOT NULL,
    recipient_id BINARY(16) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at DATETIME(6),
    read_at DATETIME(6),
    action_url VARCHAR(500),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    delivery_channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    expires_at DATETIME(6),
    PRIMARY KEY (id)
);

-- Soft delete index
CREATE INDEX idx_notifications_deleted_at ON notifications (deleted_at);

-- Common query indexes
CREATE INDEX idx_notifications_recipient ON notifications (recipient_type, recipient_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_type ON notifications (notification_type);
CREATE INDEX idx_notifications_expires ON notifications (expires_at);

-- Grant permissions (assuming default schema)
GRANT SELECT, INSERT, UPDATE, DELETE ON notifications TO veggofresh;