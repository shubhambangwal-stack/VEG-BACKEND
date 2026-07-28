package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    List<Wishlist> findByUserId(UUID userId);

    Optional<Wishlist> findByUserIdAndProductId(UUID userId, UUID productId);

    // GAP 15 — filter wishlist by product category (category is a field on ProductDto, resolved in service)
    // Note: Since Product entity lives in Vendor module, we resolve category in the service layer,
    // not via a DB join. This query just loads all wishlist items for the user.
    long countByUserId(UUID userId);
}
