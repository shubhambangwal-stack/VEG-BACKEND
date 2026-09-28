package com.veggofresh.customer.service.impl;

import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderSettlementDto;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.CustomerOrderService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.notification.entity.NotificationRecipientRole;
import com.veggofresh.notification.entity.NotificationType;
import com.veggofresh.notification.service.NotificationService;
import com.veggofresh.payment.service.PaymentService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.service.ShopLookupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REBUILT THIS ROUND -- the vendor-accept/reject broadcast redesign. Previously
 * acceptOrder/rejectOrder took no shopId at all, meaning nothing ever recorded WHO
 * actually won an order -- every original candidate stayed able to see and act on it
 * forever, including after another vendor had already accepted it. Confirmed as a real
 * bug via live testing, not caught by review. Full design discussion and reasoning in
 * NOTES_CUSTOMER.md.
 *
 * PHASE 1 FIX (earlier round, still true): moved from package
 * {@code com.veggofresh.customer.service} to {@code com.veggofresh.customer.service.impl}.
 * Delegates to the shared {@link OrderResponseMapper} instead of a duplicate mapper.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderResponseMapper orderResponseMapper;
    private final PaymentService paymentService;
    private final PlatformSettingsService platformSettingsService;
    private final NotificationService notificationService;
    private final ShopLookupService shopLookupService;

    /**
     * Real atomic accept, now recording WHO won directly in the same statement
     * (acceptedShopId) -- this is the actual fix for the root-cause bug: every other
     * vendor-facing check downstream (getAcceptedOrdersForShop, markReadyForPickup,
     * etc.) is scoped against this field, not candidacy, so a losing vendor genuinely
     * stops being able to see or act on the order the instant this succeeds for someone
     * else.
     */
    @Override
    public void acceptOrder(UUID orderId, UUID shopId) {
        log.info("Shop {} accepting order {}", shopId, orderId);

        int claimed = orderRepository.atomicAccept(orderId, shopId, OrderStatus.CONFIRMED, OrderStatus.PLACED);
        if (claimed > 0) {
            // Confirmed: notify payment service, the customer, and the winning shop owner.
            paymentService.onOrderAccepted(orderId);

            orderRepository.findById(orderId).ifPresent(order -> {
                notificationService.send(order.getUserId(), NotificationRecipientRole.CUSTOMER, NotificationType.ORDER_CONFIRMED,
                        "Order confirmed", "Your order " + order.getOrderNumber() + " has been confirmed by the shop",
                        orderData(order));
                shopLookupService.findOwnerUserIdByShopId(shopId).ifPresent(ownerId ->
                        notificationService.send(ownerId, NotificationRecipientRole.VENDOR, NotificationType.ORDER_ACCEPTED,
                                "Order accepted", "You accepted order " + order.getOrderNumber(),
                                orderData(order)));
            });
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("ORDER_NOT_ACCEPTABLE",
                    "This order is no longer in a state that can be accepted", HttpStatus.BAD_REQUEST);
        }
        // Any other status means someone else's accept already won the race.
        throw new BusinessException("ORDER_ALREADY_ACCEPTED",
                "Someone else already accepted this order", HttpStatus.CONFLICT);
    }

    /**
     * REBUILT THIS ROUND -- previously cancelled the WHOLE order the instant any one
     * candidate declined, a leftover from the old single-vendor model. Now narrows the
     * candidate pool instead: adds shopId to rejectedShopIds, and only actually cancels
     * if that empties the remaining pool (every original candidate has now rejected).
     * If other candidates remain, the order stays PLACED and broadcasting to them.
     */
    @Override
    public void rejectOrder(UUID orderId, UUID shopId) {
        log.info("Shop {} rejecting order {}", shopId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessException("ORDER_NOT_REJECTABLE",
                    "This order is no longer awaiting acceptance", HttpStatus.BAD_REQUEST);
        }
        if (!order.getCandidateVendorIds().contains(shopId)) {
            throw new BusinessException("ORDER_NOT_A_CANDIDATE",
                    "This order was never broadcast to your shop", HttpStatus.FORBIDDEN);
        }

        order.getRejectedShopIds().add(shopId);
        orderRepository.save(order);

        Set<UUID> stillLive = new HashSet<>(order.getCandidateVendorIds());
        stillLive.removeAll(order.getRejectedShopIds());

        if (stillLive.isEmpty()) {
            log.warn("Every candidate vendor declined order {} -- cancelling", orderId);
            cancelOrderSystemInitiated(orderId, "Every vendor this order was broadcast to declined it");
        }
    }

    @Override
    public void updateOrderStatus(UUID orderId, String status) {
        log.info("Updating order {} to status {}", orderId, status);
        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        orderService.updateOrderStatus(orderId, newStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrderRequestsForShop(UUID shopId) {
        int timeoutSeconds = platformSettingsService.getVendorAcceptTimeoutSeconds();
        return orderRepository.findRequestsForShop(shopId, OrderStatus.PLACED).stream()
                .map(order -> {
                    OrderResponseDto dto = orderResponseMapper.mapToDto(order);
                    // Same deadline VendorAcceptTimeoutSweepService itself uses to decide
                    // when to auto-cancel -- surfaced here so the vendor app can show a
                    // live countdown instead of the timeout being invisible.
                    dto.setVendorAcceptExpiresAt(order.getCreatedAt().plusSeconds(timeoutSeconds));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAcceptedOrdersForShop(UUID shopId) {
        return orderRepository.findByAcceptedShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(orderResponseMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void assignDeliveryAgent(UUID orderId, String agentName, String agentPhone,
                                     String agentPhotoUrl, String estimatedWindow) {
        log.info("Assigning delivery agent {} to order {}", agentName, orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found"));
        order.setDeliveryAgentName(agentName);
        order.setDeliveryAgentPhone(agentPhone);
        order.setDeliveryAgentPhotoUrl(agentPhotoUrl);
        order.setEstimatedDeliveryWindow(estimatedWindow);
        orderRepository.save(order);
    }

    @Override
    public void markDelivered(UUID orderId, String deliveryPhotoUrl, String locationNote) {
        log.info("Marking order {} as delivered", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found"));
        order.setDeliveryPhotoUrl(deliveryPhotoUrl);
        order.setDeliveryLocationNote(locationNote);
        order.setDeliveredAt(Instant.now());
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    @Override
    public void setDropOtpAvailable(UUID orderId, String dropOtp) {
        log.info("Drop OTP now available for order {} (delivery partner arrived at drop)", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found"));
        order.setDropOtp(dropOtp);
        orderRepository.save(order);
    }

    @Override
    public void cancelOrderSystemInitiated(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return; // idempotent no-op
        }

        log.warn("System-initiated cancellation for order {}: {}", orderId, reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        Order saved = orderRepository.save(order);

        notificationService.send(saved.getUserId(), NotificationRecipientRole.CUSTOMER, NotificationType.ORDER_CANCELLED,
                "Your order was cancelled",
                "Order " + saved.getOrderNumber() + " could not be fulfilled — " + reason,
                orderData(saved));

        // Same rule as OrderServiceImpl.cancelOrder(): the payment module owns the
        // money, because only it knows whether the hold was captured. This used to
        // credit saved.getTotalAmount() unconditionally, which paid out a full
        // refund for orders that were never charged, and double-refunded the ones
        // that were.
        paymentService.onOrderCancelled(saved.getId());
    }

    /**
     * Reconciles the N orders covered by a payment that failed.
     *
     * <p>Without this, a failed payment left every one of its orders in PLACED.
     * That is not a cosmetic inconsistency: PLACED is a state a vendor can
     * accept and delivery can complete, so settlements were paid out to vendors,
     * drivers and the platform for orders that were never paid for -- while the
     * customer was told the payment had failed.
     *
     * <p>Each order is cancelled through
     * {@link #cancelOrderSystemInitiated(UUID, String)}, which is idempotent and
     * notifies the customer. It also calls {@code paymentService.onOrderCancelled()},
     * which correctly voids the line: a failed payment was never captured, so
     * there is nothing to refund.
     */
    @Override
    @Transactional
    public void onPaymentFailed(UUID paymentOrderId, UUID userId, List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            // A wallet top-up has no orders behind it.
            log.info("Payment {} failed with no associated orders -- payment side already reconciled",
                    paymentOrderId);
            return;
        }

        log.warn("Payment {} failed; cancelling {} order(s) it was covering", paymentOrderId, orderIds.size());

        for (UUID orderId : orderIds) {
            try {
                cancelOrderSystemInitiated(orderId, "payment could not be completed");
            } catch (Exception e) {
                // One order must not stop the rest: leaving the others in PLACED
                // is the exact bug this method exists to close. The payment side
                // is already FAILED and cannot be retried, so the failure is
                // logged loudly for manual reconciliation.
                log.error("Failed to cancel order {} after payment {} failed: {}", orderId, paymentOrderId,
                        e.getMessage(), e);
            }
        }
    }

    private String orderData(Order order) {
        return "{\"orderId\":\"" + order.getId() + "\",\"orderNumber\":\"" + order.getOrderNumber() + "\"}";
    }

    /**
     * NEW THIS ROUND -- catches the case rejectOrder()'s own cancel-on-empty-pool
     * doesn't: some candidates never touch the order at all (don't accept, don't
     * reject), so the pool never empties on its own, but the customer's still waiting.
     * Mirrors Delivery's expireStaleAssignments/@Scheduled pattern exactly. Uses
     * vendorAcceptTimeoutSeconds specifically (not the rebroadcast-rounds setting --
     * that one's shaped for Delivery's multi-round rediscovery model, which doesn't
     * apply here since a vendor order's candidate pool is fixed once at checkout, never
     * regenerated).
     */
    @Scheduled(fixedDelay = 15000)
    public void expireStaleOrderRequests() {
        try {
            int timeoutSeconds = platformSettingsService.getVendorAcceptTimeoutSeconds();
            Instant cutoff = Instant.now().minus(timeoutSeconds, ChronoUnit.SECONDS);

            List<Order> stale = orderRepository.findByStatusAndAcceptedShopIdIsNullAndCreatedAtBefore(OrderStatus.PLACED, cutoff);
            for (Order order : stale) {
                cancelOrderSystemInitiated(order.getId(), "No vendor accepted this order within the allowed time");
            }
        } catch (Exception e) {
            log.error("Error while expiring stale order requests: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getDeliveryOtp(UUID orderId) {
        int code = Math.abs(orderId.hashCode() % 9000) + 1000;
        return String.valueOf(code);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSettlementDto getOrderForSettlement(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found for settlement: " + orderId));
        return OrderSettlementDto.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .deliveryFee(order.getDeliveryFee())
                .estimatedTax(order.getEstimatedTax())
                .acceptedShopId(order.getAcceptedShopId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderByIdForFulfillment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found: " + orderId));
        return orderResponseMapper.mapToDto(order);
    }
}
