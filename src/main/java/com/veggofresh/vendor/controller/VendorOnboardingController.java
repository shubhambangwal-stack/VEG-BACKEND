package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.VendorBasicInfoRequestDto;
import com.veggofresh.vendor.dto.request.VendorBusinessLocationRequestDto;
import com.veggofresh.vendor.dto.response.VendorOnboardingChecklistResponseDto;
import com.veggofresh.vendor.dto.response.VendorOnboardingStatusResponseDto;
import com.veggofresh.vendor.service.VendorOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REPLACES the old VendorOnboardingController (BusinessAddressRequestDto /
 * ApplicationStatusResponseDto / mapKycStatusToFlutterStatus). Delete the old file
 * and its two now-unused DTOs when merging this in -- see NOTES_VENDOR.md.
 */
@RestController
@RequestMapping("/api/vendor/onboarding")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorOnboardingController {

    private final VendorOnboardingService vendorOnboardingService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<VendorOnboardingStatusResponseDto>> getStatus() {
        var status = vendorOnboardingService.getStatus(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(status, "Onboarding status retrieved successfully"));
    }

    @PutMapping("/basic-info")
    public ResponseEntity<ApiResponse<VendorOnboardingStatusResponseDto>> submitBasicInfo(
            @Valid @RequestBody VendorBasicInfoRequestDto request) {
        var status = vendorOnboardingService.submitBasicInfo(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(status, "Basic info saved"));
    }

    @PutMapping("/business-location")
    public ResponseEntity<ApiResponse<VendorOnboardingStatusResponseDto>> submitBusinessLocation(
            @Valid @RequestBody VendorBusinessLocationRequestDto request) {
        var status = vendorOnboardingService.submitBusinessLocation(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(status, "Business location saved"));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<VendorOnboardingStatusResponseDto>> submitApplication() {
        var status = vendorOnboardingService.submitApplication(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(status, "Application submitted for review"));
    }

    @GetMapping("/checklist")
    public ResponseEntity<ApiResponse<VendorOnboardingChecklistResponseDto>> getChecklist() {
        var checklist = vendorOnboardingService.getChecklist(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(checklist, "Checklist retrieved successfully"));
    }
}
