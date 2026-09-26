package com.veggofresh.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSendRequestDto {
    private UUID recipientId;
    private String recipientType;
    private String notificationType;
    private String title;
    private String message;
    private String payload;
    private String priority;
    private String deliveryChannel;
    private Instant expiresAt;
}