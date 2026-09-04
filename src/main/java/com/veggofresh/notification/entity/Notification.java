package com.veggofresh.notification.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

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
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class Notification extends BaseEntity {

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
    private boolean isRead;
}