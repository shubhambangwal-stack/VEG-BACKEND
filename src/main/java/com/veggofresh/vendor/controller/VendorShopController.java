package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.ShopRegistrationRequestDto;
import com.veggofresh.vendor.dto.request.ShopUpdateRequestDto;
import com.veggofresh.vendor.dto.response.ShopDto;
import com.veggofresh.vendor.service.VendorShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/shop")
@RequiredArgsConstructor
public class VendorShopController {

    private final VendorShopService vendorShopService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShopDto>> registerShop(@Valid @RequestBody ShopRegistrationRequestDto request) {
        // If ownerUserId is not provided, use the current user
        if (request.getOwnerUserId() == null) {
            request.setOwnerUserId(SecurityUtils.getCurrentUserId());
        }
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.registerShop(request), "Shop registered successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ShopDto>> getShopProfile() {
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.getShopByOwner(SecurityUtils.getCurrentUserId()), "Shop profile retrieved"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ShopDto>> updateShopProfile(@Valid @RequestBody ShopUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.updateShop(SecurityUtils.getCurrentUserId(), request), "Shop updated successfully"));
    }

    @PostMapping("/kyc-documents")
    public ResponseEntity<ApiResponse<Void>> submitKycDocuments() {
        vendorShopService.submitKycDocuments(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("KYC documents submitted successfully"));
    }
}
