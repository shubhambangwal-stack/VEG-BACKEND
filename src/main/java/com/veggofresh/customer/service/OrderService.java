package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.dto.response.CheckoutResultDto;
import com.veggofresh.customer.dto.response.CheckoutSummaryDto;
import com.veggofresh.customer.dto.response.InvoiceDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderTrackingResponseDto;
import com.veggofresh.customer.dto.response.RatingResponseDto;
import com.veggofresh.customer.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    /**
     * PHASE 2 — processes ALL of the customer's open carts in one call.
     * Each cart that still validates (live vendor-overlap re-check) becomes
     * its own independent Order; any cart that no longer validates is
     * reported in the result's issues list and left untouched rather than
     * blocking the rest (PROJECT_STATE section 2).
     */
    CheckoutResultDto checkout(UUID userId, OrderRequestDto request);

    Page<OrderResponseDto> getOrderHistory(UUID userId, Pageable pageable);

    Page<OrderResponseDto> getOrderHistoryByStatusGroup(UUID userId, String statusGroup, Pageable pageable);

    OrderResponseDto getOrderDetails(UUID userId, UUID orderId);
    OrderTrackingResponseDto trackOrder(UUID userId, UUID orderId);
    RatingResponseDto rateOrder(UUID userId, UUID orderId, RatingRequestDto request);
    OrderResponseDto updateOrderStatus(UUID orderId, OrderStatus newStatus);
    OrderResponseDto cancelOrder(UUID userId, UUID orderId);

    /**
     * PHASE 2 — CHANGED BEHAVIOR: reorder no longer creates an Order
     * directly. Under the multi-cart model, only checkout() creates Orders
     * (it's the one place that does live vendor-overlap re-validation), so
     * reorder now re-adds the original order's items back through the same
     * add-to-cart logic used everywhere else, and returns the resulting
     * open carts. The customer reviews and checks out again, same as any
     * other cart. This mirrors the "no automatic retry" principle already
     * locked for Payment (PROJECT_STATE section 6).
     */
    List<CartResponseDto> reorder(UUID userId, UUID orderId);

    InvoiceDto getInvoice(UUID userId, UUID orderId);

    /** PHASE 2 — per-cart breakdown + grand total across all open carts. */
    CheckoutSummaryDto getCheckoutSummary(UUID userId, UUID addressId);
}
