package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);

    /** PHASE 2 — look up an item by id across ANY of the user's open carts. */
    Optional<CartItem> findByIdAndCart_UserId(UUID id, UUID userId);
}
