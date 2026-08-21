package com.veggofresh.customer.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @Column(name = "order_number", unique = true, length = 20)
    private String orderNumber;

    // ── Fee Breakdown ────────────────────────────────────────
    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "estimated_tax", precision = 10, scale = 2)
    private BigDecimal estimatedTax;

    // ── Payment Reference ────────────────────────────────────
    @Column(name = "payment_method_id", length = 36)
    private String paymentMethodId;

    // ── Scheduled Delivery ───────────────────────────────────
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "delivery_time_slot", length = 30)
    private String deliveryTimeSlot;

    // ── Delivery Agent (populated by Delivery module) ────────
    @Column(name = "delivery_agent_name", length = 100)
    private String deliveryAgentName;

    @Column(name = "delivery_agent_phone", length = 20)
    private String deliveryAgentPhone;

    @Column(name = "delivery_agent_photo_url", columnDefinition = "TEXT")
    private String deliveryAgentPhotoUrl;

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

    @Column(name = "delivery_location_note", length = 100)
    private String deliveryLocationNote;

    // ── Promo Code ───────────────────────────────────────────
    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "promo_discount", precision = 10, scale = 2)
    private BigDecimal promoDiscount;

    // ── Multi-cart / vendor-broadcast prep (PROJECT_STATE NEW ARCHITECTURE §2-3) ──
    /**
     * The cart this order was created from. The cart itself is soft-deleted
     * immediately after successful conversion; kept here purely for audit/debug.
     */
    @Column(name = "source_cart_id")
    private UUID sourceCartId;

    /**
     * Vendor candidates re-validated fresh at checkout time. Every order has
     * exactly one vendor by construction (§3) — this set is the pool the
     * future simultaneous-broadcast + atomic-accept flow (owned by Vendor
     * module) will broadcast to. Now also the source of truth
     * OrderRepository.findByShopId() reads from directly (fixed — see that
     * query's own comment for the bug this closes).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_candidate_vendors", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "vendor_id")
    private Set<UUID> candidateVendorIds = new HashSet<>();
}
