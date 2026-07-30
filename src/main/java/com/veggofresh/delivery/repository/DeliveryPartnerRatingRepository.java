package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryPartnerRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryPartnerRatingRepository extends JpaRepository<DeliveryPartnerRating, UUID> {
    Optional<DeliveryPartnerRating> findByAssignmentId(UUID assignmentId);
    List<DeliveryPartnerRating> findByDeliveryPartnerUserId(UUID deliveryPartnerUserId);
}
