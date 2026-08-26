package com.veggofresh.customer.service.impl;

import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.payment.service.WalletTransactionReason;
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
 * than Admin's configured {@code vendorAcceptTimeoutSeconds}. For each, calls
 * PaymentService.onOrderCancelled() (which decides void vs wallet refund based
 * on payment state) and then cancels the order.
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
    private final WalletService walletService;

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

                    walletService.credit(order.getUserId(), order.getTotalAmount(), WalletTransactionReason.ORDER_CANCELLED_REFUND,
                            order.getId(), "Refund for timeout-cancelled order " + order.getOrderNumber());

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
