package com.veggofresh.notification.entity;

import com.veggofresh.platform.common.BaseEntity;
<<<<<<< HEAD
import jakarta.persistence.*;
=======
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

<<<<<<< HEAD
import java.time.Instant;
import java.util.UUID;

=======
import java.util.UUID;

/**
 * A single persisted notification. Created via
 * {@code NotificationService.send(...)} which ALWAYS saves the row before any
 * real-time delivery attempt, so a notification is never lost when the
 * recipient is offline — the REST endpoints (initial load / fallback) read the
 * exact same data the socket would have delivered.
 *
 * <p>{@code data} is an opaque JSON string (order/assignment/payment ids, shop
 * ids, amounts …) written by the caller and forwarded verbatim to clients.
 */
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class Notification extends BaseEntity {

<<<<<<< HEAD
    @Column(name = "recipient_type", nullable = false)
    private String recipientType;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "priority", nullable = false)
    private String priority;

    @Column(name = "delivery_channel", nullable = false)
    private String deliveryChannel;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public enum Type {
        VENDOR_BROADCAST,
        DELIVERY_BROADCAST,
        PICKUP_OTP,
        STATUS_UPDATE,
        PAYMENT_EVENT,
        WALLET_DEBIT,
        WALLET_RELEASE,
        ORDER_ACCEPTED,
        ORDER_REJECTED,
        ORDER_CANCELLED,
        NEW_ORDER,
        PROMO_APPLIED,
        LOW_STOCK_ALERT
    }

    public enum Status {
        PENDING,
        SENT,
        READ,
        EXPIRED,
        FAILED
    }

    public enum Channel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
=======
    /** Recipient's auth User UUID — loose cross-module reference (no FK). */
    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, updatable = false, length = 20)
    private NotificationRecipientRole recipientRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, updatable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String body;

    /** Caller-supplied JSON context forwarded verbatim to clients. */
    @Column(columnDefinition = "TEXT", updatable = false)
    private String data;

    @Column(name = "is_read", nullable = false)
    private boolean read;
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
}