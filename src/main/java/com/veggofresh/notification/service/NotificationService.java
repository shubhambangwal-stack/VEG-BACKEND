package com.veggofresh.notification.service;

import com.veggofresh.notification.dto.NotificationSendRequestDto;
import com.veggofresh.notification.dto.NotificationResponseDto;
import com.veggofresh.notification.dto.NotificationStatusRequestDto;
import com.veggofresh.notification.dto.NotificationCountRequestDto;
import com.veggofresh.notification.dto.NotificationCountResponseDto;
import com.veggofresh.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationService {

    Notification sendNotification(NotificationSendRequestDto request);

    Optional<NotificationResponseDto> getNotificationStatus(UUID notificationId);

    NotificationCountResponseDto getNotificationCount(NotificationCountRequestDto request);

    List<NotificationResponseDto> getNotificationsByRecipient(String recipientType, UUID recipientId, Integer page, Integer size);

    Optional<NotificationResponseDto> markAsRead(UUID notificationId);

    Optional<NotificationResponseDto> deleteNotification(UUID notificationId);
}