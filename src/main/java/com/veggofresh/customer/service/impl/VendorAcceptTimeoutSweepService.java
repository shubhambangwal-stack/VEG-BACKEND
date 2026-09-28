package com.veggofresh.customer.service.impl;

import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Closes the audit gap: "vendorAcceptTimeoutSeconds is configured in Admin but
 * nothing reads it; only Delivery has a @Scheduled sweep".
 *
 * Runs every 30 seconds. Finds PLACED orders whose {@code createdAt} is older
 * than Admin's configured {@code vendorAcceptTimeoutSeconds}. For each, cancels
 * the order and calls PaymentService.onOrderCancelled(), which decides between
 * voiding the payment line and refunding it based on whether money was actually
 * captured. This service does not move money itself.
 *
 * Lives in the Customer module because it owns the {@link Order} entity and the
 * OrderRepository. Depends on Payment module for the cancel hook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorAcceptTimeoutSweepService {

    private final OrderRepository orderRepository;
    private final PlatformSettingsService platformSettingsService;
    private final PaymentService paymentService;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void sweepExpiredOrders() {
        try {
            int timeoutSeconds = platformSettingsService.getVendorAcceptTimeoutSeconds();
            Instant cutoff = Instant.now().minus(timeoutSeconds, ChronoUnit.SECONDS);

            List<Order> timedOut = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PLACED, cutoff);
            if (timedOut.isEmpty()) return;

            log.info("VendorAcceptTimeoutSweep: {} PLACED order(s) expired (timeout={}s)", timedOut.size(), timeoutSeconds);

            for (Order order : timedOut) {
                try {
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setCancelledAt(Instant.now());
                    orderRepository.save(order);

                    // Delegate the money to the payment module, exactly as the
                    // customer-initiated cancel path does. This method used to
                    // credit the order total unconditionally, which meant an
                    // order whose payment hold was never captured still paid the
                    // customer a full refund in wallet credit -- free money for
                    // anyone who simply let an order time out. onOrderCancelled()
                    // voids the line when nothing was captured and refunds only
                    // when it was.
                    //
                    // The class javadoc above already described this call as the
                    // behaviour; the PaymentService import was present but never
                    // wired to a field, so the intent was documented and the
                    // implementation was lost.
                    paymentService.onOrderCancelled(order.getId());

                    log.warn("Order {} timed out waiting for vendor accept -- cancelled", order.getId());
                } catch (Exception e) {
                    log.error("Failed to cancel timed-out order {}: {}", order.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("VendorAcceptTimeoutSweep error: {}", e.getMessage(), e);
        }
    }
}
