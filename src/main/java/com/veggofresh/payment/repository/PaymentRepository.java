package com.veggofresh.payment.repository;

import com.veggofresh.payment.entity.Payment;
import com.veggofresh.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** Find payment by Razorpay order ID — primary lookup in verify + webhook flows. */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /** All payment attempts for a VegGoFresh order (may be multiple on retries). */
    List<Payment> findByOrderId(UUID orderId);

    /** All payment attempts by a user. */
    List<Payment> findByUserId(UUID userId);

    /**
     * Check if a CAPTURED payment already exists for an order.
     * Used for idempotency — prevents double-capturing from duplicate webhooks.
     */
    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
