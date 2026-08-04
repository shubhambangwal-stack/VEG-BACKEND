package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.VendorShopRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorShopRatingRepository extends JpaRepository<VendorShopRating, UUID> {
    Optional<VendorShopRating> findByOrderIdAndShopId(UUID orderId, UUID shopId);
    List<VendorShopRating> findByShopId(UUID shopId);
}
