package com.veggofresh.notification.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class Notification extends BaseEntity {

    @Column(name = "recipient_type", nullable = false)
    private String recipientType;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "priority", nullable = false)
    private String priority;

    @Column(name = "delivery_channel", nullable = false)
    private String deliveryChannel;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public enum Type {
        VENDOR_BROADCAST,
        DELIVERY_BROADCAST,
        PICKUP_OTP,
        STATUS_UPDATE,
        PAYMENT_EVENT,
        WALLET_DEBIT,
        WALLET_RELEASE,
        ORDER_ACCEPTED,
        ORDER_REJECTED,
        ORDER_CANCELLED,
        NEW_ORDER,
        PROMO_APPLIED,
        LOW_STOCK_ALERT
    }

    public enum Status {
        PENDING,
        SENT,
        READ,
        EXPIRED,
        FAILED
    }

    public enum Channel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
}