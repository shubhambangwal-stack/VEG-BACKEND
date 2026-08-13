package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.dto.response.CheckoutResultDto;
import com.veggofresh.customer.dto.response.CheckoutSummaryDto;
import com.veggofresh.customer.dto.response.DeliverySlotDto;
import com.veggofresh.customer.dto.response.InvoiceDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderTrackingResponseDto;
import com.veggofresh.customer.dto.response.RatingResponseDto;
import com.veggofresh.customer.service.DeliverySlotService;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final DeliverySlotService deliverySlotService;

    /**
     * PHASE 2 — BREAKING CHANGE: response is now {@link CheckoutResultDto}
     * (a list of orders + a list of issues) instead of a single
     * OrderResponseDto, since one checkout call can produce N orders — one
     * per open cart that still validates.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResultDto>> checkout(
            @Valid @RequestBody OrderRequestDto request) {
        CheckoutResultDto result = orderService.checkout(SecurityUtils.getCurrentUserId(), request);
        String message = result.getIssues().isEmpty()
                ? "Order placed successfully"
                : "Order placed for available items — some carts need your attention";
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDto>>> getOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponseDto> history;
        if (status != null && !status.trim().isEmpty()) {
            history = orderService.getOrderHistoryByStatusGroup(SecurityUtils.getCurrentUserId(), status, PageRequest.of(page, size));
        } else {
            history = orderService.getOrderHistory(SecurityUtils.getCurrentUserId(), PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(history), "Order history retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderDetails(
            @PathVariable UUID id) {
        OrderResponseDto order = orderService.getOrderDetails(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order details retrieved successfully"));
    }

    @GetMapping("/{id}/track")
    public ResponseEntity<ApiResponse<OrderTrackingResponseDto>> trackOrder(
            @PathVariable UUID id) {
        OrderTrackingResponseDto tracking = orderService.trackOrder(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(tracking, "Order tracking information retrieved successfully"));
    }

    @PostMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<RatingResponseDto>> rateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody RatingRequestDto request) {
        RatingResponseDto rating = orderService.rateOrder(SecurityUtils.getCurrentUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(rating, "Order rated successfully"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDto>> cancelOrder(
            @PathVariable UUID id) {
        OrderResponseDto order = orderService.cancelOrder(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    /**
     * PHASE 2 — CHANGED BEHAVIOR: reorder no longer places an order directly.
     * It re-adds the original order's items back into the cart system (same
     * multi-cart logic as any add-to-cart call) and returns the resulting
     * open carts. The customer reviews and calls checkout() again, same as
     * any other cart — see OrderService.reorder() javadoc for why.
     */
    @PostMapping("/{id}/reorder")
    public ResponseEntity<ApiResponse<List<CartResponseDto>>> reorder(
            @PathVariable UUID id) {
        List<CartResponseDto> carts = orderService.reorder(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(carts, "Order items added back to your cart — review and checkout when ready"));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable UUID id) {
        InvoiceDto invoice = orderService.getInvoice(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice generated successfully"));
    }

    /** PHASE 2 — response shape changed to a per-cart breakdown + grand total. */
    @GetMapping("/checkout/summary")
    public ResponseEntity<ApiResponse<CheckoutSummaryDto>> getCheckoutSummary(
            @RequestParam UUID addressId) {
        CheckoutSummaryDto summary = orderService.getCheckoutSummary(SecurityUtils.getCurrentUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Checkout summary retrieved successfully"));
    }

    @GetMapping("/delivery-slots")
    public ResponseEntity<ApiResponse<List<DeliverySlotDto>>> getDeliverySlots(
            @RequestParam(required = false) String date) {
        List<DeliverySlotDto> slots = deliverySlotService.getAvailableSlots(date);
        return ResponseEntity.ok(ApiResponse.success(slots, "Delivery slots retrieved successfully"));
    }
}
