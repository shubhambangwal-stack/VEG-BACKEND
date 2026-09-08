package com.veggofresh.customer.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class CustomerProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * Local, editable copy of the customer's email — same workaround pattern as
     * Vendor's {@code Shop.email} and Delivery's {@code DeliveryPartnerProfile.email}.
     * Auth's {@code User} entity has no cross-module write path, so email lives here
     * once the customer sets it via the profile endpoint. Optional; not the login
     * identifier (phone remains that, sourced read-only from Auth via UserLookupService).
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Cloudinary public_id of the current avatar, required to delete the old asset when
     * it is replaced. Internal bookkeeping only — never exposed on any response DTO.
     */
    @Column(name = "avatar_public_id", length = 500)
    private String avatarPublicId;
}
