package com.veggofresh.delivery.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_assignments")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryAssignment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Null while the assignment is unclaimed (PENDING). */
    @Column(name = "delivery_partner_user_id")
    private UUID deliveryPartnerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeliveryAssignmentStatus status = DeliveryAssignmentStatus.PENDING;

    @Column(name = "pickup_latitude", nullable = false)
    private double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private double pickupLongitude;

    @Column(name = "drop_latitude", nullable = false)
    private double dropLatitude;

    @Column(name = "drop_longitude", nullable = false)
    private double dropLongitude;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // ── Phase B: denormalized fields for contact info ──────────────────
    // customerUserId/shopOwnerUserId let Delivery resolve LIVE phone numbers via
    // UserLookupService at read time (no staleness risk).
    // shopName/shopAddress are a SNAPSHOT taken at dispatch time -- Vendor module
    // has no ShopLookupService yet, so this can't be resolved live. See NOTES.md.
    @Column(name = "customer_user_id")
    private UUID customerUserId;

    @Column(name = "shop_owner_user_id")
    private UUID shopOwnerUserId;

    @Column(name = "shop_name", length = 255)
    private String shopName;

    @Column(name = "shop_address", length = 500)
    private String shopAddress;
}
