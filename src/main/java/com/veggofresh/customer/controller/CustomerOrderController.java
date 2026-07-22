package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.request.OrderRequestDto;
import com.veggofresh.customer.dto.request.RatingRequestDto;
import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.customer.dto.response.OrderTrackingResponseDto;
import com.veggofresh.customer.dto.response.RatingResponseDto;
import com.veggofresh.customer.service.OrderService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> checkout(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto order = orderService.checkout(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order placed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDto>>> getOrderHistory(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponseDto> history = orderService.getOrderHistory(UUID.fromString(userId), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(history), "Order history retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderDetails(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {
        OrderResponseDto order = orderService.getOrderDetails(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order details retrieved successfully"));
    }

    @GetMapping("/{id}/track")
    public ResponseEntity<ApiResponse<OrderTrackingResponseDto>> trackOrder(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {
        OrderTrackingResponseDto tracking = orderService.trackOrder(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success(tracking, "Order tracking information retrieved successfully"));
    }

    @PostMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<RatingResponseDto>> rateOrder(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id,
            @Valid @RequestBody RatingRequestDto request) {
        RatingResponseDto rating = orderService.rateOrder(UUID.fromString(userId), id, request);
        return ResponseEntity.ok(ApiResponse.success(rating, "Order rated successfully"));
    }
}
