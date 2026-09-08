package com.veggofresh.notification.entity;

/**
 * Recipient identity for a notification. Mirrors the platform's four
 * authenticated roles (auth {@code UserRole}) but lives in the Notification
 * module so the notification engine never depends on a foreign module's
 * entity enum.
 */
public enum NotificationRecipientRole {
    CUSTOMER,
    VENDOR,
    DELIVERY,
    ADMIN
}