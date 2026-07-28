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

import java.util.UUID;

/**
 * Delivery partner profile. Cross-module identity reference is by userId (UUID) only —
 * never a JPA relationship into com.veggofresh.auth.entity.User.
 */
@Entity
@Table(name = "delivery_partners")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryPartnerProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 50)
    private DeliveryKycStatus kycStatus = DeliveryKycStatus.PENDING;

    @Column(name = "is_online", nullable = false)
    private boolean online = false;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "vehicle_type", length = 50)
    private String vehicleType;
}
