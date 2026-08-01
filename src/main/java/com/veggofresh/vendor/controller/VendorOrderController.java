package com.veggofresh.vendor.controller;

import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.service.VendorOrderManagementService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/orders")
@RequiredArgsConstructor
public class VendorOrderController {

    private final VendorOrderManagementService vendorOrderManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getOrders() {
        List<OrderResponseDto> orders = vendorOrderManagementService.getShopOrders(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptOrder(@PathVariable UUID id) {
        vendorOrderManagementService.acceptOrder(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order accepted successfully"));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectOrder(@PathVariable UUID id) {
        vendorOrderManagementService.rejectOrder(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        vendorOrderManagementService.updateOrderStatus(SecurityUtils.getCurrentUserId(), id, request.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully"));
    }
}
