package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.response.VendorProfileStatsResponseDto;
import com.veggofresh.vendor.service.VendorStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorProfileController {

    private final VendorStatsService vendorStatsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<VendorProfileStatsResponseDto>> getProfileStats() {
        var stats = vendorStatsService.getProfileStats(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(stats, "Profile stats retrieved successfully"));
    }
}
