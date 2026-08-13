package com.veggofresh.customer.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // ── Order Number ─────────────────────────────────────────
    /** Human-readable order number, e.g. "#VG-2940582". Auto-generated at checkout. */
    @Column(name = "order_number", unique = true, length = 20)
    private String orderNumber;

    // ── Fee Breakdown ────────────────────────────────────────
    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "estimated_tax", precision = 10, scale = 2)
    private BigDecimal estimatedTax;

    // ── Payment Reference ────────────────────────────────────
    /** UUID reference only — PaymentMethod entity lives in the Payment module. */
    @Column(name = "payment_method_id", length = 36)
    private String paymentMethodId;

    /**
     * Razorpay order ID (format: {@code order_XXXX}).
     * Set when a Razorpay payment order is created ({@code POST /api/payment/orders}).
     * Null for COD or pre-payment-module orders.
     */
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    /**
     * Denormalized mirror of {@code Payment.status} for quick read without join.
     * Values: {@code CREATED}, {@code CAPTURED}, {@code FAILED}, or null for legacy orders.
     * Updated by PaymentService on each payment lifecycle transition.
     */
    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    // ── Scheduled Delivery ───────────────────────────────────
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    /** e.g. "09:00 - 11:00" */
    @Column(name = "delivery_time_slot", length = 30)
    private String deliveryTimeSlot;

    // ── Delivery Agent (populated by Delivery module) ────────
    @Column(name = "delivery_agent_name", length = 100)
    private String deliveryAgentName;

    @Column(name = "delivery_agent_phone", length = 20)
    private String deliveryAgentPhone;

    @Column(name = "delivery_agent_photo_url", columnDefinition = "TEXT")
    private String deliveryAgentPhotoUrl;

    /** e.g. "2:30 PM – 4:00 PM" */
    @Column(name = "estimated_delivery_window", length = 50)
    private String estimatedDeliveryWindow;

    // ── Status Timestamps ────────────────────────────────────
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "preparing_at")
    private Instant preparingAt;

    @Column(name = "out_for_delivery_at")
    private Instant outForDeliveryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // ── Proof of Delivery ────────────────────────────────────
    @Column(name = "delivery_photo_url", columnDefinition = "TEXT")
    private String deliveryPhotoUrl;

    /** e.g. "Front Door", "Reception" */
    @Column(name = "delivery_location_note", length = 100)
    private String deliveryLocationNote;

    // ── Promo Code ───────────────────────────────────────────
    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "promo_discount", precision = 10, scale = 2)
    private BigDecimal promoDiscount;
}
