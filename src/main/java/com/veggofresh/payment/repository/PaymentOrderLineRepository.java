package com.veggofresh.payment.repository;

import com.veggofresh.payment.entity.PaymentOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderLineRepository extends JpaRepository<PaymentOrderLine, UUID> {

    /** One line per Order by construction (unique index on order_id) -- this is the accept/reject/cancel hook's entry point. */
    Optional<PaymentOrderLine> findByOrderId(UUID orderId);

    /** All lines for a batch -- read under the parent PaymentOrder's row lock (see PaymentOrderRepository.findByIdForUpdate), never independently locked. */
    List<PaymentOrderLine> findByPaymentOrderId(UUID paymentOrderId);
}
