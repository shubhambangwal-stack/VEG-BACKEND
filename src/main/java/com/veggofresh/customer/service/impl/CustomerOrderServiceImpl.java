package com.veggofresh.customer.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PHASE 1 FIX: moved from package {@code com.veggofresh.customer.service} to
 * {@code com.veggofresh.customer.service.impl} — every other impl class in
 * this module lives under {@code .service.impl}; this one didn't, for no
 * apparent reason. Also now delegates to the shared {@link OrderResponseMapper}
 * instead of carrying its own duplicate copy of the Order -> DTO mapping.
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

    /**
     * FIXED THIS ROUND -- real atomic accept for the vendor-accept race. Previously
     * delegated straight to orderService.updateOrderStatus(orderId, CONFIRMED), a plain
     * read-then-write with no protection against two vendors genuinely racing to accept
     * the same order (legitimate when candidateVendorIds holds more than one vendor --
     * see OrderRepository.findByShopId's own comment for when that happens). Same
     * pattern as Delivery's atomicClaim(): one conditional UPDATE, not a check-then-set;
     * the loser gets a clean ORDER_ALREADY_ACCEPTED (409), not a silent double-accept or
     * an unhandled exception.
     */
    @Override
    public void acceptOrder(UUID orderId) {
        log.info("Accepting order {}", orderId);

        int claimed = orderRepository.atomicAccept(orderId, OrderStatus.CONFIRMED, OrderStatus.PLACED);
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

    @Override
    public void rejectOrder(UUID orderId) {
        log.info("Rejecting order {}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    @Override
    public void updateOrderStatus(UUID orderId, String status) {
        log.info("Updating order {} to status {}", orderId, status);
        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        orderService.updateOrderStatus(orderId, newStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByShopId(UUID shopId) {
        log.info("Fetching orders for shop {}", shopId);
        List<Order> orders = orderRepository.findByShopId(shopId);
        return orders.stream()
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

    /**
     * NEW THIS ROUND -- called by Delivery when a re-broadcast limit is hit (see
     * DeliveryAssignmentServiceImpl.reassign()). Deliberately more permissive than the
     * customer-facing cancelOrder(): no ownership check (system caller, not a specific
     * customer), no status-range restriction (a re-broadcast limit can be hit from
     * READY_FOR_PICKUP or OUT_FOR_DELIVERY, not just PLACED/CONFIRMED). Already-terminal
     * orders are a silent no-op rather than an error, since the caller is a background
     * process that shouldn't need to pre-check state before calling this.
     */
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

    @Override
    @Transactional(readOnly = true)
    public String getDeliveryOtp(UUID orderId) {
        int code = Math.abs(orderId.hashCode() % 9000) + 1000;
        return String.valueOf(code);
    }
}
