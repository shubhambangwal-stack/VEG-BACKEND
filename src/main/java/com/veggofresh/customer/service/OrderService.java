package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
import com.veggofresh.customer.dto.response.CheckoutSummaryDto;
import com.veggofresh.customer.dto.response.InvoiceDto;
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

    /** GAP 12 — filter by status group: ALL, IN_PROGRESS, DELIVERED, CANCELLED */
    Page<OrderResponseDto> getOrderHistoryByStatusGroup(UUID userId, String statusGroup, Pageable pageable);

    OrderResponseDto getOrderDetails(UUID userId, UUID orderId);
    OrderTrackingResponseDto trackOrder(UUID userId, UUID orderId);
    RatingResponseDto rateOrder(UUID userId, UUID orderId, RatingRequestDto request);
    OrderResponseDto updateOrderStatus(UUID orderId, OrderStatus newStatus);

    /** GAP 11 — cancel an order (PLACED or CONFIRMED only) */
    OrderResponseDto cancelOrder(UUID userId, UUID orderId);

    /** GAP 11 — re-order from a previously DELIVERED order */
    OrderResponseDto reorder(UUID userId, UUID orderId);

    /** GAP 11 — get invoice/receipt for a completed order */
    InvoiceDto getInvoice(UUID userId, UUID orderId);

    /** GAP 17 — compute checkout summary before placing order */
    CheckoutSummaryDto getCheckoutSummary(UUID userId, UUID addressId);
}
