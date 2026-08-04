package com.veggofresh.vendor.controller;

import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.response.VendorOrderDetailResponseDto;
import com.veggofresh.vendor.service.VendorOrderManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MODIFIED: added GET /{id} for the enriched Figma "Order Details" screen, and added
 * @PreAuthorize at class level -- this controller had NO auth check at all before
 * (flagged in the original audit); fixed here since the file was being touched anyway
 * for the new endpoint. Closes one item off the "deliberately not touched" list in
 * NOTES_VENDOR.md.
 */
@RestController
@RequestMapping("/api/vendor/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorOrderController {

    private final VendorOrderManagementService vendorOrderManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getOrders() {
        List<OrderResponseDto> orders = vendorOrderManagementService.getShopOrders(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorOrderDetailResponseDto>> getOrderDetail(@PathVariable UUID id) {
        var detail = vendorOrderManagementService.getOrderDetail(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(detail, "Order detail retrieved successfully"));
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
