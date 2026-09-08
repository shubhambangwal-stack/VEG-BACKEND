package com.veggofresh.notification.service;

import com.veggofresh.notification.dto.NotificationDto;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Public cross-module notification entry point. Any module (order / vendor /
 * delivery / payment / admin) can call {@link #send(...)} from their own flow —
 * the notification is PERSISTED FIRST, then pushed over STOMP if the recipient
 * is connected, and is also retrievable later through the REST endpoints.
 *
 * <p>Synchronous and dependency-light by design — no message queue. This is
 * the "get it working before the deadline" version; the row in
 * {@code notifications} is the durable source of truth either way.
 */
public interface NotificationService {

    /**
     * Persists a notification for one recipient and attempts a real-time push
     * to {@code /user/queue/notifications} (no-op if the user has no live
     * socket session).
     *
     * @param recipientId    the recipient's auth User UUID
     * @param recipientRole  CUSTOMER | VENDOR | DELIVERY | ADMIN
     * @param type           machine-readable {@link NotificationType}
     * @param title          short human-readable title
     * @param body           optional longer description
     * @param dataJson       optional opaque JSON context, forwarded verbatim
     * @return the persisted notification DTO (what clients receive)
     */
    NotificationDto send(UUID recipientId, NotificationRecipientRole recipientRole,
                         NotificationType type, String title, String body, String dataJson);

    /**
     * Admin broadcast. {@code recipientRole} null/blank/{@code ALL} → every
     * user of every role; otherwise filtered to one role. Returns how many
     * recipients were notified.
     */
    int broadcast(String title, String body, String dataJson, String recipientRole);

    /** Newest-first notification inbox for the authenticated recipient. */
    Page<NotificationDto> getNotifications(UUID recipientId, Pageable pageable);

    /** Unread badge count for the authenticated recipient. */
    long getUnreadCount(UUID recipientId);

    /**
     * Marks one notification read. Only the row's own recipient can do this.
     *
     * @throws com.veggofresh.platform.exception.BusinessException NOTIFICATION_NOT_FOUND
     *         if the id doesn't exist or isn't owned by the caller
     */
    void markAsRead(UUID recipientId, UUID notificationId);

    /** Marks every notification of the recipient as read; returns how many rows were updated. */
    int markAllAsRead(UUID recipientId);
}