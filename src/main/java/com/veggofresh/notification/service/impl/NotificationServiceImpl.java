package com.veggofresh.notification.service.impl;

<<<<<<< HEAD
import com.veggofresh.notification.dto.NotificationSendRequestDto;
import com.veggofresh.notification.dto.NotificationResponseDto;
import com.veggofresh.notification.dto.NotificationStatusRequestDto;
import com.veggofresh.notification.dto.NotificationCountRequestDto;
import com.veggofresh.notification.dto.NotificationCountResponseDto;
import com.veggofresh.notification.entity.Notification;
import com.veggofresh.notification.repository.NotificationRepository;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;

@Service
public class NotificationServiceImpl implements NotificationService {

    @PersistenceContext
    private EntityManager entityManager;

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public Notification sendNotification(NotificationSendRequestDto request) {
        Notification notification = new Notification();
        notification.setRecipientType(request.getRecipientType());
        notification.setRecipientId(request.getRecipientId() != null ? request.getRecipientId() : UUID.randomUUID());
        notification.setNotificationType(request.getNotificationType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setPayload(request.getPayload());
        notification.setStatus(Notification.Status.PENDING.name());
        notification.setPriority(request.getPriority() != null ? request.getPriority() : "NORMAL");
        notification.setDeliveryChannel(request.getDeliveryChannel() != null ? request.getDeliveryChannel() : Notification.Channel.IN_APP.name());
        notification.setSentAt(Instant.now());
        if (request.getExpiresAt() != null) {
            notification.setExpiresAt(request.getExpiresAt());
        } else {
            notification.setExpiresAt(Instant.now().plusSeconds(86400));
        }

        notification = notificationRepository.save(notification);

        // TODO: Actually send via the configured delivery channel (email/SMS/push)
        // This would integrate with email service, SMS gateway, etc.
        notification.setStatus(Notification.Status.SENT.name());
        notification.setSentAt(Instant.now());
        notification = notificationRepository.save(notification);

        return notification;
    }

    @Override
    public Optional<NotificationResponseDto> getNotificationStatus(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    NotificationResponseDto dto = new NotificationResponseDto();
                    dto.setId(notification.getId());
                    dto.setTitle(notification.getTitle());
                    dto.setMessage(notification.getMessage());
                    dto.setStatus(notification.getStatus());
                    dto.setDeliveredAt(notification.getSentAt() != null ? notification.getSentAt().toString() : null);
                    dto.setReadAt(notification.getReadAt() != null ? notification.getReadAt().toString() : null);
                    dto.setActionUrl(notification.getActionUrl());
                    return dto;
                });
    }

    @Override
    public NotificationCountResponseDto getNotificationCount(NotificationCountRequestDto request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Notification> root = query.from(Notification.class);

        Predicate statusFilter = cb.equal(root.get("status"), request.getStatus() != null ? request.getStatus() : "PENDING");
        Predicate recipientFilter = cb.equal(root.get("recipientType"), request.getRecipientType());

        if (request.getRecipientId() != null) {
            recipientFilter = cb.and(recipientFilter, cb.equal(root.get("recipientId"), request.getRecipientId()));
        }

        query.where(statusFilter, recipientFilter);
        long total = entityManager.createQuery(query).getSingleResult();

        // Count by status
        long pending = countByStatus(cb, "PENDING", request);
        long sent = countByStatus(cb, "SENT", request);
        long read = countByStatus(cb, "READ", request);
        long failed = countByStatus(cb, "FAILED", request);
        long expired = countByStatus(cb, "EXPIRED", request);

        return NotificationCountResponseDto.builder()
                .pendingCount(pending)
                .sentCount(sent)
                .readCount(read)
                .failedCount(failed)
                .expiredCount(expired)
                .build();
    }

    private long countByStatus(CriteriaBuilder cb, String status, NotificationCountRequestDto request) {
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Notification> root = query.from(Notification.class);

        Predicate statusFilter = cb.equal(root.get("status"), status);
        Predicate recipientFilter = cb.equal(root.get("recipientType"), request.getRecipientType());

        if (request.getRecipientId() != null) {
            recipientFilter = cb.and(recipientFilter, cb.equal(root.get("recipientId"), request.getRecipientId()));
        }

        query.where(statusFilter, recipientFilter);
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public List<NotificationResponseDto> getNotificationsByRecipient(String recipientType, UUID recipientId, Integer page, Integer size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Notification> query = cb.createQuery(Notification.class);
        Root<Notification> root = query.from(Notification.class);

        Predicate recipientFilter = cb.equal(root.get("recipientType"), recipientType);
        Predicate idFilter = cb.equal(root.get("recipientId"), recipientId);

        query.where(recipientFilter, idFilter);
        query.orderBy(cb.desc(root.get("sentAt")));

        // Apply pagination manually since we're using EntityManager directly
        // In a full Spring Data JPA setup, use Pageable
        List<Notification> results = entityManager.createQuery(query).getResultList();

        return results.stream().map(notification -> {
            NotificationResponseDto dto = new NotificationResponseDto();
            dto.setId(notification.getId());
            dto.setTitle(notification.getTitle());
            dto.setMessage(notification.getMessage());
            dto.setStatus(notification.getStatus());
            dto.setDeliveredAt(notification.getSentAt() != null ? notification.getSentAt().toString() : null);
            dto.setReadAt(notification.getReadAt() != null ? notification.getReadAt().toString() : null);
            dto.setActionUrl(notification.getActionUrl());
            return dto;
        }).collect(Collectors.toList());
=======
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
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
    }

    @Override
    @Transactional
<<<<<<< HEAD
    public Optional<NotificationResponseDto> markAsRead(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notification.setStatus(Notification.Status.READ.name());
                    notification.setReadAt(Instant.now());
                    notification = notificationRepository.save(notification);

                    NotificationResponseDto dto = new NotificationResponseDto();
                    dto.setId(notification.getId());
                    dto.setTitle(notification.getTitle());
                    dto.setMessage(notification.getMessage());
                    dto.setStatus(notification.getStatus());
                    dto.setReadAt(notification.getReadAt() != null ? notification.getReadAt().toString() : null);
                    return dto;
                });
=======
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
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
    }

    @Override
    @Transactional
<<<<<<< HEAD
    public Optional<NotificationResponseDto> deleteNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    notificationRepository.delete(notification);

                    NotificationResponseDto dto = new NotificationResponseDto();
                    dto.setId(notification.getId());
                    dto.setTitle(notification.getTitle());
                    dto.setMessage(notification.getMessage());
                    dto.setStatus(notification.getStatus());
                    return dto;
                });
=======
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
>>>>>>> 5d59f32924e5d18dc9e8d7fe3f7ff5cb7a78a1a2
    }
}