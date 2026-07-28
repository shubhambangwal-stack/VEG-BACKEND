package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryAssignment;
import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {
    Optional<DeliveryAssignment> findByOrderIdAndStatusIn(UUID orderId, List<DeliveryAssignmentStatus> statuses);
    List<DeliveryAssignment> findByDeliveryPartnerUserIdAndStatus(UUID deliveryPartnerUserId, DeliveryAssignmentStatus status);
    List<DeliveryAssignment> findByStatusAndExpiresAtBefore(DeliveryAssignmentStatus status, Instant instant);
    List<DeliveryAssignment> findByOrderId(UUID orderId);
}
