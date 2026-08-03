package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.VendorAccountSettingsRequestDto;
import com.veggofresh.vendor.dto.response.VendorAccountSettingsResponseDto;
import com.veggofresh.vendor.service.VendorShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/account-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorAccountSettingsController {

    private final VendorShopService vendorShopService;

    @GetMapping
    public ResponseEntity<ApiResponse<VendorAccountSettingsResponseDto>> getSettings() {
        var settings = vendorShopService.getAccountSettings(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(settings, "Account settings retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<VendorAccountSettingsResponseDto>> updateSettings(
            @Valid @RequestBody VendorAccountSettingsRequestDto request) {
        var settings = vendorShopService.updateAccountSettings(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Account settings updated successfully"));
    }
}
