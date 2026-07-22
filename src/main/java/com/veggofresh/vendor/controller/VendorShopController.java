package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
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

    // Typically the user ID would be extracted from the SecurityContext
    // For simplicity, we are passing it explicitly or assuming a mock for now
    private UUID getCurrentUserId() {
        // Mock ID for development without full security context
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShopDto>> registerShop(@Valid @RequestBody ShopRegistrationRequestDto request) {
        // If ownerUserId is not provided, use the current user
        if (request.getOwnerUserId() == null) {
            request.setOwnerUserId(getCurrentUserId());
        }
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.registerShop(request), "Shop registered successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ShopDto>> getShopProfile() {
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.getShopByOwner(getCurrentUserId()), "Shop profile retrieved"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ShopDto>> updateShopProfile(@Valid @RequestBody ShopUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.updateShop(getCurrentUserId(), request), "Shop updated successfully"));
    }

    @PostMapping("/kyc-documents")
    public ResponseEntity<ApiResponse<Void>> submitKycDocuments() {
        vendorShopService.submitKycDocuments(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("KYC documents submitted successfully"));
    }
}
