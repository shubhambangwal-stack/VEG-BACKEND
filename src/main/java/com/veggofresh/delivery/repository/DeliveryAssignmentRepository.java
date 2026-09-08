package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryAssignment;
import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {
    Optional<DeliveryAssignment> findByOrderIdAndStatusIn(UUID orderId, List<DeliveryAssignmentStatus> statuses);
    List<DeliveryAssignment> findByDeliveryPartnerUserIdAndStatus(UUID deliveryPartnerUserId, DeliveryAssignmentStatus status);
    List<DeliveryAssignment> findByStatusAndExpiresAtBefore(DeliveryAssignmentStatus status, Instant instant);
    List<DeliveryAssignment> findByOrderId(UUID orderId);

    /**
     * NEW -- real atomic accept. A single conditional UPDATE, not a read-then-write:
     * only succeeds (returns 1) if the row is STILL in expectedStatus at the moment the
     * UPDATE runs. If two delivery partners race to accept the same assignment, exactly
     * one UPDATE affects a row; the other affects zero rows and the caller turns that
     * into a clean "someone else already accepted" response instead of silently
     * corrupting state or relying on catching an OptimisticLockException after the fact.
     */
    @Modifying
    @Query("UPDATE DeliveryAssignment a SET a.status = :newStatus, a.deliveryPartnerUserId = :partnerId " +
           "WHERE a.id = :id AND a.status = :expectedStatus")
    int atomicClaim(@Param("id") UUID id, @Param("partnerId") UUID partnerId,
                     @Param("newStatus") DeliveryAssignmentStatus newStatus,
                     @Param("expectedStatus") DeliveryAssignmentStatus expectedStatus);

    /** NEW -- how many rounds (PENDING assignment rows) this order has gone through so far, across all re-broadcasts. */
    long countByOrderId(UUID orderId);

    /** NEW -- the very first round's row, whose createdAt anchors the "total elapsed time across all rounds" cap. */
    Optional<DeliveryAssignment> findFirstByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
