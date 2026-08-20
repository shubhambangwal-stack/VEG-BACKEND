package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryOtp;
import com.veggofresh.delivery.entity.DeliveryOtpType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryOtpRepository extends JpaRepository<DeliveryOtp, UUID> {
    Optional<DeliveryOtp> findByAssignmentIdAndType(UUID assignmentId, DeliveryOtpType type);
}
