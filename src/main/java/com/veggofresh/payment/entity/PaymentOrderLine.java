package com.veggofresh.payment.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per Customer {@code Order}, always belonging to exactly one
 * {@link PaymentOrder} batch (an Order is created once, by exactly one
 * checkout() call, so it can never belong to more than one payment).
 *
 * {@code orderId} is a loose UUID reference only -- same convention as
 * {@code WalletTransaction.referenceId} and {@code DeliveryAssignment
 * .orderId} -- never a JPA relation to Customer's {@code Order} entity.
 */
@Entity
@Table(name = "payment_order_lines")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class PaymentOrderLine extends BaseEntity {

    @Column(name = "payment_order_id", nullable = false)
    private UUID paymentOrderId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentOrderLineStatus status = PaymentOrderLineStatus.PENDING;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
