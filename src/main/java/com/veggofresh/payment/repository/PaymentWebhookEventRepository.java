package com.veggofresh.payment.repository;

import com.veggofresh.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    Optional<PaymentWebhookEvent> findByRazorpayEventId(String razorpayEventId);

    boolean existsByRazorpayEventId(String razorpayEventId);
}
