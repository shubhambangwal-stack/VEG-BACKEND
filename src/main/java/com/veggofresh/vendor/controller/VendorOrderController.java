package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
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

    private UUID getCurrentUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Object>>> getOrders() {
        // Mock returning empty list of orders since customer module owns orders
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Orders retrieved successfully"));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptOrder(@PathVariable UUID id) {
        vendorOrderManagementService.acceptOrder(getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order accepted successfully"));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectOrder(@PathVariable UUID id) {
        vendorOrderManagementService.rejectOrder(getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(@PathVariable UUID id, @RequestBody Map<String, String> request) {
        vendorOrderManagementService.updateOrderStatus(getCurrentUserId(), id, request.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully"));
    }
}
