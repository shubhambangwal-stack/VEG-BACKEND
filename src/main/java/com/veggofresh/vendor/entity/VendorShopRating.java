package com.veggofresh.vendor.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * Customer's rating of a shop, scoped to (orderId, shopId) rather than just orderId --
 * a single order can span multiple vendors (nothing scopes a cart to one shop, same
 * situation as Order Details' item filtering), so each shop in a multi-vendor order
 * gets rated independently. Distinct from Customer module's own order/product Rating
 * entity and from Delivery's DeliveryPartnerRating -- confirmed as a separate concept,
 * same reasoning as that decision. See VendorRatingService and NOTES_VENDOR.md.
 */
@Entity
@Table(name = "vendor_shop_ratings")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class VendorShopRating extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "customer_user_id", nullable = false)
    private UUID customerUserId;

    @Column(name = "rating_value", nullable = false)
    private int ratingValue;

    @Column(length = 1000)
    private String comment;
}
