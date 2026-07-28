package com.veggofresh.delivery.controller;

import com.veggofresh.delivery.dto.request.OtpVerifyRequestDto;
import com.veggofresh.delivery.dto.response.DeliveryAssignmentResponseDto;
import com.veggofresh.delivery.service.DeliveryAssignmentService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY')")
public class DeliveryOrderController {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentResponseDto>>> getNearbyOrders(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "5") double radiusKm) {
        var assignments = deliveryAssignmentService.getNearbyAssignments(SecurityUtils.getCurrentUserId(), lat, lng, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(assignments, "Nearby assignments retrieved successfully"));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponseDto>> accept(@PathVariable UUID id) {
        var assignment = deliveryAssignmentService.acceptAssignment(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(assignment, "Assignment accepted"));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponseDto>> reject(@PathVariable UUID id) {
        var assignment = deliveryAssignmentService.rejectAssignment(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(assignment, "Assignment rejected"));
    }

    @PutMapping("/{id}/pickup")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponseDto>> pickup(@PathVariable UUID id) {
        var assignment = deliveryAssignmentService.markPickedUp(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(assignment, "Order marked as picked up"));
    }

    @PostMapping("/{id}/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@PathVariable UUID id, @Valid @RequestBody OtpVerifyRequestDto request) {
        deliveryAssignmentService.verifyDeliveryOtp(SecurityUtils.getCurrentUserId(), id, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponseDto>> complete(@PathVariable UUID id) {
        var assignment = deliveryAssignmentService.completeDelivery(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(assignment, "Delivery completed successfully"));
    }
}
