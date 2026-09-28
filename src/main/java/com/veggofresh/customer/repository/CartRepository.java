package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * PHASE 2 — a customer can have several open carts now, oldest first
     * ("Cart 1, Cart 2, ...").
     *
     * <p>Ordering is by {@code createdAt} then {@code id}. The tiebreak on id
     * is load-bearing: carts created inside the same millisecond previously came
     * back in an arbitrary order, which made the "Cart N" labels shuffle between
     * two reads of the same data. Labels are what the client uses to tie a
     * checkout-summary breakdown back to a cart, so they have to be stable.
     */
    List<Cart> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    Optional<Cart> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Badge count in one query: the sum of quantities across every open cart.
     *
     * <p>Deliberately does NOT load the Cart graph. The old implementation
     * loaded every open cart and every cart's item collection just to add up a
     * number. Soft-deleted carts are excluded via Cart's {@code @Where}, and
     * soft-deleted items via CartItem's, so retired carts and lines never
     * contribute.
     */
    @Query("SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.userId = :userId")
    int sumItemQuantities(@Param("userId") UUID userId);
}
