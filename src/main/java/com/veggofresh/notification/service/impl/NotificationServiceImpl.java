package com.veggofresh.notification.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.notification.dto.NotificationDto;
import com.veggofresh.notification.entity.Notification;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import com.veggofresh.notification.entity.UserDeviceToken;
import com.veggofresh.notification.repository.NotificationRepository;
import com.veggofresh.notification.repository.UserDeviceTokenRepository;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.notification.service.fcm.FcmService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Durable-first notification engine with real-time STOMP & FCM Push Notification delivery.
 *
 * <p>Order of operations is non-negotiable: the notification row is saved to
 * {@code notifications} BEFORE any delivery attempt, so a recipient who is
 * offline (or whose socket died) still has the notification when they hydrate
 * via {@code GET /api/notifications}. The STOMP & FCM pushes are fast-path side
 * effects, not the source of truth.
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
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserLookupService userLookupService;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

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
        Notification saved = notificationRepository.saveAndFlush(notification);

        NotificationDto dto = NotificationDto.from(saved);

        // 2) Fast-path push: STOMP websocket + FCM Push Notifications
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
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
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
        // A) STOMP WebSocket Push
        try {
            messagingTemplate.convertAndSendToUser(recipientId.toString(), QUEUE_NOTIFICATIONS, dto);
            log.debug("Pushed notification {} to /user/queue/notifications for user {}", dto.getId(), recipientId);
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {} (notification {} remains persisted): {}",
                    recipientId, dto.getId(), e.getMessage());
        }

        // B) FCM Push Notification (Mobile/Web Background Push)
        try {
            if (fcmService.isInitialized()) {
                List<UserDeviceToken> deviceTokens = userDeviceTokenRepository.findByUserId(recipientId);
                if (!deviceTokens.isEmpty()) {
                    List<String> tokens = deviceTokens.stream()
                            .map(UserDeviceToken::getFcmToken)
                            .collect(Collectors.toList());

                    Map<String, String> fcmDataMap = buildFcmDataMap(dto);

                    List<String> invalidTokens = fcmService.sendToMultipleTokens(
                            tokens, dto.getTitle(), dto.getBody(), fcmDataMap);

                    // Purge stale/unregistered device tokens
                    if (invalidTokens != null && !invalidTokens.isEmpty()) {
                        invalidTokens.forEach(staleToken -> {
                            userDeviceTokenRepository.deleteByFcmToken(staleToken);
                            log.info("Purged unregistered FCM device token: {}", staleToken);
                        });
                    }
                }
            }
        } catch (Exception e) {
            log.warn("FCM push failed for user {} (notification {} remains persisted): {}",
                    recipientId, dto.getId(), e.getMessage());
        }
    }

    private Map<String, String> buildFcmDataMap(NotificationDto dto) {
        Map<String, String> map = new HashMap<>();
        map.put("notificationId", dto.getId() != null ? dto.getId().toString() : "");
        map.put("type", dto.getType() != null ? dto.getType().name() : "");
        map.put("recipientRole", dto.getRecipientRole() != null ? dto.getRecipientRole().name() : "");

        if (dto.getData() != null && !dto.getData().isBlank()) {
            try {
                Map<String, Object> jsonMap = objectMapper.readValue(
                        dto.getData(), new TypeReference<Map<String, Object>>() {});
                jsonMap.forEach((k, v) -> {
                    if (k != null && v != null) {
                        map.put(k, v.toString());
                    }
                });
            } catch (Exception e) {
                map.put("rawJson", dto.getData());
            }
        }
        return map;
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