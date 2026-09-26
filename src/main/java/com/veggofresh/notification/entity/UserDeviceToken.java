package com.veggofresh.notification.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores registered FCM (Firebase Cloud Messaging) device tokens per user for
 * push notification delivery.
 */
@Entity
@Table(name = "user_device_tokens")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class UserDeviceToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 512)
    private String fcmToken;

    @Column(nullable = false, length = 20)
    private String platform = "ANDROID";

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
