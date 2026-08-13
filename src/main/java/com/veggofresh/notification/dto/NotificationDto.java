package com.veggofresh.notification.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private UUID id;
    private String title;
    private String message;
    private String status;
    private String deliveredAt;
    private String readAt;
    private String actionUrl;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusRequestDto {
    private UUID notificationId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDto {
    private Long totalElements;
    private Long totalPages;
    private Integer size;
    private Integer number;
    private Boolean last;
    private java.util.List<NotificationResponseDto> content;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountRequestDto {
    private String recipientType;
    private UUID recipientId;
    private String status;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequestDto {
    private String token;
    private String platform; // ANDROID, IOS, WEB
    private UUID userId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountResponseDto {
    private Long pendingCount;
    private Long sentCount;
    private Long readCount;
    private Long failedCount;
    private Long expiredCount;
}