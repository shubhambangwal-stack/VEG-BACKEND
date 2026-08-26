package com.veggofresh.payment.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.Instant;

/**
 * Idempotency record for inbound Razorpay webhooks -- see V132's migration
 * comment. {@code razorpayEventId} is unique; PaymentWebhookServiceImpl
 * checks for an existing row before doing any real work, and always writes
 * this row (even on events it otherwise ignores) so a redelivered event is
 * recognized on the very next call.
 */
@Entity
@Table(name = "payment_webhook_events")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class PaymentWebhookEvent extends BaseEntity {

    @Column(name = "razorpay_event_id", nullable = false, length = 64)
    private String razorpayEventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processed_at")
    private Instant processedAt;
}
