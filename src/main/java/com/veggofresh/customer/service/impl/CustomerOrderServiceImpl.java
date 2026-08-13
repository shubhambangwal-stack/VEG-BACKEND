package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.entity.Order;
import com.veggofresh.customer.entity.OrderStatus;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.service.CustomerOrderService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.platform.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void acceptOrder(UUID orderId) {
        log.info("Accepting order {}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
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

    @Override
    @Transactional(readOnly = true)
    public String getDeliveryOtp(UUID orderId) {
        int code = Math.abs(orderId.hashCode() % 9000) + 1000;
        return String.valueOf(code);
    }
}
