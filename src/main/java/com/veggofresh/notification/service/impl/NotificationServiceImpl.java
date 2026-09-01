package com.veggofresh.notification.service.impl;

import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.notification.dto.NotificationDto;
import com.veggofresh.notification.entity.Notification;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import com.veggofresh.notification.repository.NotificationRepository;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Durable-first notification engine.
 *
 * <p>Order of operations is non-negotiable: the notification row is saved to
 * {@code notifications} BEFORE any delivery attempt, so a recipient who is
 * offline (or whose socket died) still has the notification when they hydrate
 * via {@code GET /api/notifications}. The STOMP push is a fast-path side
 * effect, not the source of truth.
 *
 * <p>Because {@link #send} is normally invoked from inside a caller-owned
 * transaction, the DB write shares that transaction (no {@code REQUIRES_NEW},
 * no message queue) — acceptable for this stage and keeps everything in sync
 * with the business action that produced the event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final String QUEUE_NOTIFICATIONS = "/queue/notifications";
    private static final List<String> ALL_ROLES = List.of(
            NotificationRecipientRole.CUSTOMER.name(),
            NotificationRecipientRole.VENDOR.name(),
            NotificationRecipientRole.DELIVERY.name(),
            NotificationRecipientRole.ADMIN.name());

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserLookupService userLookupService;

    @Override
    @Transactional
    public NotificationDto send(UUID recipientId, NotificationRecipientRole recipientRole,
                                NotificationType type, String title, String body, String dataJson) {
        if (recipientId == null || recipientRole == null || type == null) {
            throw new BusinessException("NOTIFICATION_INVALID_RECIPIENT",
                    "recipientId, recipientRole and type are required", HttpStatus.BAD_REQUEST);
        }

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setRecipientRole(recipientRole);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setData(dataJson);
        notification.setRead(false);

        // 1) Durable write first — nothing is lost if delivery fails.
        // saveAndFlush guarantees the INSERT is physically issued BEFORE the
        // STOMP push below, honouring the persist-then-deliver contract even
        // though this usually runs inside a caller-owned transaction.
        Notification saved = notificationRepository.saveAndFlush(notification);

        NotificationDto dto = NotificationDto.from(saved);

        // 2) Fast-path push to the recipient's private queue if connected.
        push(recipientId, dto);
        return dto;
    }

    @Override
    @Transactional
    public int broadcast(String title, String body, String dataJson, String recipientRole) {
        if (recipientRole == null || recipientRole.isBlank() || "ALL".equalsIgnoreCase(recipientRole)) {
            int total = 0;
            for (String role : ALL_ROLES) {
                total += broadcastToRole(roleToEnum(role), title, body, dataJson);
            }
            return total;
        }
        return broadcastToRole(roleToEnum(recipientRole.toUpperCase()), title, body, dataJson);
    }

    @Override
    public Page<NotificationDto> getNotifications(UUID recipientId, Pageable pageable) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationDto::from);
    }

    @Override
    public long getUnreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID recipientId, UUID notificationId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND",
                        "Notification not found or does not belong to you", HttpStatus.NOT_FOUND));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID recipientId) {
        return notificationRepository.markAllRead(recipientId);
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private int broadcastToRole(NotificationRecipientRole role, String title, String body, String dataJson) {
        List<UUID> userIds = userLookupService.findUserIdsByRole(role.name());
        userIds.forEach(userId -> send(userId, role, NotificationType.ADMIN_ANNOUNCEMENT, title, body, dataJson));
        log.info("Admin broadcast delivered to {} {} user(s)", userIds.size(), role);
        return userIds.size();
    }

    private void push(UUID recipientId, NotificationDto dto) {
        try {
            // user destination → `/user/queue/notifications` on the client.
            // The user name must match StompPrincipal.name (= user UUID string).
            messagingTemplate.convertAndSendToUser(recipientId.toString(), QUEUE_NOTIFICATIONS, dto);
            log.debug("Pushed notification {} to /user/queue/notifications for user {}", dto.getId(), recipientId);
        } catch (Exception e) {
            // Delivery failure must never fail the business transaction that
            // produced the notification — the row is already durable.
            log.warn("WebSocket push failed for user {} (notification {} remains persisted): {}",
                    recipientId, dto.getId(), e.getMessage());
        }
    }

    private NotificationRecipientRole roleToEnum(String role) {
        try {
            return NotificationRecipientRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("NOTIFICATION_INVALID_ROLE",
                    "Unknown recipient role: " + role, HttpStatus.BAD_REQUEST);
        }
    }
}