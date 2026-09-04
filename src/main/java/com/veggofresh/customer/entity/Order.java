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

    /**
     * Real drop-off OTP, pushed here by Delivery ({@code CustomerOrderService.setDropOtpAvailable})
     * the moment it's generated -- initial issuance right when the delivery partner
     * marks "picked up", or later if regenerated -- null before that. Replaces the old
     * fake {@code CustomerOrderService.getDeliveryOtp()} hashCode-derived stand-in,
     * which is now dead code left in place for compatibility.
     */
    @Column(name = "drop_otp", length = 10)
    private String dropOtp;

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

    // ── Vendor accept/reject broadcast tracking (this round) ──────────────
    /**
     * NULL until a vendor wins the accept race, then permanent. This is the ONLY
     * source of truth for "who actually has this order" — candidateVendorIds is just
     * the original broadcast list and never changes after checkout. Every vendor-side
     * check that gates real actions (mark ready for pickup, order history, status
     * updates) must check this field, not candidateVendorIds — checking candidacy
     * instead of acceptance was the root cause of a real bug: a vendor who lost the
     * accept race could still see and act on an order they never actually won.
     */
    @Column(name = "accepted_shop_id")
    private UUID acceptedShopId;

    /**
     * Shops that have explicitly declined this order. Rejecting narrows the
     * candidate pool (candidateVendorIds minus rejectedShopIds = who's still live) —
     * it does NOT cancel the order by itself. The order only cancels when this set
     * grows to cover every original candidate, or the accept timeout elapses first,
     * whichever comes first (see CustomerOrderServiceImpl's sweep).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_rejected_shops", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "shop_id")
    private Set<UUID> rejectedShopIds = new HashSet<>();
}
