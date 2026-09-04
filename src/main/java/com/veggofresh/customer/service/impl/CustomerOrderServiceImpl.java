package com.veggofresh.customer.service.impl;

import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.CustomerOrderService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.payment.service.WalletService;
import com.veggofresh.payment.service.WalletTransactionReason;
import com.veggofresh.platform.exception.BusinessException;

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
    private final WalletService walletService;
    private final PlatformSettingsService platformSettingsService;

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
        return orderRepository.findRequestsForShop(shopId, OrderStatus.PLACED).stream()
                .map(orderResponseMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAcceptedOrdersForShop(UUID shopId) {
        return orderRepository.findByAcceptedShopId(shopId).stream()
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

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            log.info("cancelOrderSystemInitiated no-op for order {} -- already terminal ({})", orderId, order.getStatus());
            return;
        }

        log.warn("System-initiated cancellation for order {}: {}", orderId, reason);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        Order saved = orderRepository.save(order);

        walletService.credit(saved.getUserId(), saved.getTotalAmount(), WalletTransactionReason.ORDER_CANCELLED_REFUND,
                saved.getId(), reason);
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
}
