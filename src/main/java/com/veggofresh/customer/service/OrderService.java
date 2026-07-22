package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderTrackingResponseDto;
import com.veggofresh.customer.dto.response.RatingResponseDto;
import com.veggofresh.customer.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderResponseDto checkout(UUID userId, OrderRequestDto request);
    Page<OrderResponseDto> getOrderHistory(UUID userId, Pageable pageable);
    OrderResponseDto getOrderDetails(UUID userId, UUID orderId);
    OrderTrackingResponseDto trackOrder(UUID userId, UUID orderId);
    RatingResponseDto rateOrder(UUID userId, UUID orderId, RatingRequestDto request);
    OrderResponseDto updateOrderStatus(UUID orderId, OrderStatus newStatus);
}
