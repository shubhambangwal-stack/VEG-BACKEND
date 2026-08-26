package com.veggofresh.vendor.controller;

import com.veggofresh.customer.dto.response.OrderResponseDto;
import com.veggofresh.delivery.dto.VendorDeliveryStatusDto;
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
 * EXTENDED THIS ROUND -- three new endpoints: mark-ready-for-pickup (the real dispatch
 * trigger), pickup-otp (for vendor to read and hand over), delivery-status (vendor-side
 * "where's my courier" visibility). See NOTES_VENDOR.md.
 */
@RestController
@RequestMapping("/api/vendor/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorOrderController {

    private final VendorOrderManagementService vendorOrderManagementService;

    /** NEW THIS ROUND -- the broadcast inbox. See VendorOrderManagementService.getOrderRequests(). */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getOrderRequests() {
        List<OrderResponseDto> requests = vendorOrderManagementService.getOrderRequests(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(requests, "Order requests retrieved successfully"));
    }

    /** CHANGED MEANING THIS ROUND -- now returns only orders this shop actually won the accept race for, not every order it was ever a candidate for. See VendorOrderManagementService.getShopOrders(). */
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

    /** NEW THIS ROUND -- the real dispatch trigger. See VendorOrderManagementService.markReadyForPickup(). */
    @PutMapping("/{id}/ready-for-pickup")
    public ResponseEntity<ApiResponse<Void>> markReadyForPickup(@PathVariable UUID id) {
        String message = vendorOrderManagementService.markReadyForPickup(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /** NEW THIS ROUND -- null pickupOtp (empty string in the response) means no delivery partner has accepted yet; that's an expected state, not an error. */
    @GetMapping("/{id}/pickup-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPickupOtp(@PathVariable UUID id) {
        String otp = vendorOrderManagementService.getPickupOtp(SecurityUtils.getCurrentUserId(), id);
        String message = otp != null ? "Pickup OTP retrieved successfully" : "No delivery partner has accepted this order yet";
        return ResponseEntity.ok(ApiResponse.success(Map.of("pickupOtp", otp == null ? "" : otp), message));
    }

    /** NEW THIS ROUND -- vendor-side delivery visibility, closing the gap where only Customer could see any delivery status at all. */
    @GetMapping("/{id}/delivery-status")
    public ResponseEntity<ApiResponse<VendorDeliveryStatusDto>> getDeliveryStatus(@PathVariable UUID id) {
        VendorDeliveryStatusDto status = vendorOrderManagementService.getDeliveryStatus(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(status, "Delivery status retrieved successfully"));
    }
}
