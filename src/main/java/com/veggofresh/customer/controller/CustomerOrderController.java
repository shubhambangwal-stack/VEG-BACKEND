package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
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

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> checkout(
            @Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto order = orderService.checkout(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order placed successfully"));
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

    @PostMapping("/{id}/reorder")
    public ResponseEntity<ApiResponse<OrderResponseDto>> reorder(
            @PathVariable UUID id) {
        OrderResponseDto order = orderService.reorder(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order placed from history successfully"));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable UUID id) {
        InvoiceDto invoice = orderService.getInvoice(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice generated successfully"));
    }

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
