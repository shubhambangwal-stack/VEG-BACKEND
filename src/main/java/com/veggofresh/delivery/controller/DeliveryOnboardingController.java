package com.veggofresh.delivery.controller;

import com.veggofresh.delivery.dto.request.BasicInfoRequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep3RequestDto;
import com.veggofresh.delivery.dto.response.OnboardingStatusResponseDto;
import com.veggofresh.delivery.service.DeliveryOnboardingService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/delivery/onboarding")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY')")
public class DeliveryOnboardingController {

    private final DeliveryOnboardingService deliveryOnboardingService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponseDto>> getStatus() {
        var status = deliveryOnboardingService.getStatus(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(status, "Onboarding status retrieved successfully"));
    }

    @PutMapping("/basic-info")
    public ResponseEntity<ApiResponse<OnboardingStatusResponseDto>> submitBasicInfo(@Valid @RequestBody BasicInfoRequestDto request) {
        var status = deliveryOnboardingService.submitBasicInfo(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(status, "Basic info saved"));
    }

    @PostMapping("/verification/step-1")
    public ResponseEntity<ApiResponse<OnboardingStatusResponseDto>> submitStep1(
            @RequestParam String licenseNumber,
            @RequestParam("photo") MultipartFile licensePhoto) {
        var status = deliveryOnboardingService.submitVerificationStep1(SecurityUtils.getCurrentUserId(), licenseNumber, licensePhoto);
        return ResponseEntity.ok(ApiResponse.success(status, "License details saved"));
    }

    @PostMapping("/verification/step-2")
    public ResponseEntity<ApiResponse<OnboardingStatusResponseDto>> submitStep2(
            @RequestParam String plateNumber,
            @RequestParam String vehicleModel,
            @RequestParam Integer manufactureYear,
            @RequestParam("photo") MultipartFile insurancePhoto) {
        var status = deliveryOnboardingService.submitVerificationStep2(SecurityUtils.getCurrentUserId(), plateNumber, vehicleModel, manufactureYear, insurancePhoto);
        return ResponseEntity.ok(ApiResponse.success(status, "Vehicle details saved"));
    }

    @PutMapping("/verification/step-3")
    public ResponseEntity<ApiResponse<OnboardingStatusResponseDto>> submitStep3(@Valid @RequestBody VerificationStep3RequestDto request) {
        var status = deliveryOnboardingService.submitVerificationStep3(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(status, "Application submitted for review"));
    }
}
