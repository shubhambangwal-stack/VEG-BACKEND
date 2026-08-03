package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.StoreProfileRequestDto;
import com.veggofresh.vendor.dto.response.StoreProfileResponseDto;
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
@RequestMapping("/api/vendor/store-profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorStoreProfileController {

    private final VendorShopService vendorShopService;

    @GetMapping
    public ResponseEntity<ApiResponse<StoreProfileResponseDto>> getStoreProfile() {
        var profile = vendorShopService.getStoreProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(profile, "Store profile retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<StoreProfileResponseDto>> updateStoreProfile(
            @Valid @RequestBody StoreProfileRequestDto request) {
        var profile = vendorShopService.updateStoreProfile(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Store profile updated successfully"));
    }
}
