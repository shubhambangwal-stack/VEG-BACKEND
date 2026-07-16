package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.service.CustomerProfileService;
import com.veggofresh.platform.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> getProfile(@AuthenticationPrincipal String userId) {
        CustomerProfileResponseDto profile = customerProfileService.getOrCreateProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(profile, "Customer profile retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> updateProfile(@AuthenticationPrincipal String userId) {
        CustomerProfileResponseDto profile = customerProfileService.updateProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(profile, "Customer profile updated successfully"));
    }
}
