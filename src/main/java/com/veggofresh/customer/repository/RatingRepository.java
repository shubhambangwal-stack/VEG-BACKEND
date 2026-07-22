package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByOrderId(UUID orderId);
}
