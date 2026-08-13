package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /** PHASE 2 — a customer can have several open carts now, oldest first ("Cart 1, Cart 2, ..."). */
    List<Cart> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Cart> findByIdAndUserId(UUID id, UUID userId);
}
