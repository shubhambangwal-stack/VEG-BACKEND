package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryAssignmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAssignmentStatusHistoryRepository extends JpaRepository<DeliveryAssignmentStatusHistory, UUID> {
    List<DeliveryAssignmentStatusHistory> findByAssignmentIdOrderByCreatedAtAsc(UUID assignmentId);
}
