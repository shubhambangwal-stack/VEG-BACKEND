package com.veggofresh.payment.repository;

import com.veggofresh.payment.entity.PayoutRequest;
import com.veggofresh.payment.entity.PayoutRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    Page<PayoutRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<PayoutRequest> findByStatusOrderByCreatedAtDesc(PayoutRequestStatus status, Pageable pageable);

    java.util.Optional<PayoutRequest> findByRazorpayPayoutId(String razorpayPayoutId);
}
