package com.veggofresh.notification.service.impl;

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
    }

    @Override
    @Transactional
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
    }

    @Override
    @Transactional
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
    }
}