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
    long countByUserId(UUID userId);

    /**
     * Takes a PostgreSQL transaction-scoped advisory lock for the given key and returns 1.
     *
     * <p>Used by {@code WishlistServiceImpl.addToWishlist} so that two simultaneous "add" requests
     * for the same customer + product (a double tap) run one after the other instead of both
     * passing the "already in wishlist?" check and both trying to INSERT. The lock is released
     * automatically when the surrounding transaction commits or rolls back, and it only blocks
     * requests with the SAME key -- different customers / products never wait for each other.
     *
     * <p>Must be called inside a transaction (the service is {@code @Transactional}).
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:key))) AS lock_taken",
            nativeQuery = true)
    Integer lockForAdd(@Param("key") String key);
}
