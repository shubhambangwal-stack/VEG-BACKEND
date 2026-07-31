package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryProofOfDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryProofOfDeliveryRepository extends JpaRepository<DeliveryProofOfDelivery, UUID> {
    Optional<DeliveryProofOfDelivery> findByAssignmentId(UUID assignmentId);
}
