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
import com.veggofresh.platform.security.SecurityUtils;
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
            @Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto order = orderService.checkout(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order placed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDto>>> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponseDto> history = orderService.getOrderHistory(SecurityUtils.getCurrentUserId(), PageRequest.of(page, size));
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
}
