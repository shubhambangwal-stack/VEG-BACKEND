package com.veggofresh.delivery.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * Photo + checklist proof-of-delivery, submitted alongside (not instead of) the delivery
 * OTP -- confirmed with the team both stay. photoUrl is mocked storage, same pattern as
 * DeliveryDocument (see MockFileStorageService). photo is REQUIRED to complete delivery
 * (matches the "Required" badge on the mockup); the checklist booleans and notes are
 * best-effort / optional -- nothing in the mockup marks them mandatory.
 */
@Entity
@Table(name = "delivery_proof_of_delivery")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryProofOfDelivery extends BaseEntity {

    @Column(name = "assignment_id", nullable = false, unique = true)
    private UUID assignmentId;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "delivered_to_customer_directly", nullable = false)
    private boolean deliveredToCustomerDirectly = false;

    @Column(name = "left_at_front_door", nullable = false)
    private boolean leftAtFrontDoor = false;

    @Column(name = "packaging_intact", nullable = false)
    private boolean packagingIntact = false;

    @Column(name = "address_verified_manually", nullable = false)
    private boolean addressVerifiedManually = false;

    @Column(length = 1000)
    private String notes;
}
