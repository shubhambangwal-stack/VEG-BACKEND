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
 * Customer's rating of a delivery partner, one per completed assignment. Distinct from
 * Customer module's own order/shop Rating entity -- confirmed as a separate concept.
 * The action originates on Customer's side (rate after delivery) but the data belongs
 * here. See DeliveryRatingService (public interface, Customer module's job to wire the
 * actual trigger, same pattern as DeliveryDispatchService) and NOTES.md.
 */
@Entity
@Table(name = "delivery_partner_ratings")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class DeliveryPartnerRating extends BaseEntity {

    @Column(name = "assignment_id", nullable = false, unique = true)
    private UUID assignmentId;

    @Column(name = "delivery_partner_user_id", nullable = false)
    private UUID deliveryPartnerUserId;

    @Column(name = "customer_user_id", nullable = false)
    private UUID customerUserId;

    @Column(name = "rating_value", nullable = false)
    private int ratingValue;

    @Column(length = 1000)
    private String comment;
}
