package com.veggofresh.notification.dto;

import com.veggofresh.notification.entity.Notification;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire shape for a notification: what the REST endpoints return and exactly
 * what the STOMP broker pushes to {@code /user/queue/notifications}. {@code data}
 * is transported as-is — clients may parse it when it is JSON.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private UUID id;
    private UUID recipientId;
    private NotificationRecipientRole recipientRole;
    private NotificationType type;
    private String title;
    private String body;
    private String data;
    private boolean read;
    private Instant createdAt;

    public static NotificationDto from(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .recipientRole(n.getRecipientRole())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .data(n.getData())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}