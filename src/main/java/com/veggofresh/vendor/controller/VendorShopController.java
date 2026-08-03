package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.ShopUpdateRequestDto;
import com.veggofresh.vendor.dto.response.ShopDto;
import com.veggofresh.vendor.service.VendorShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * MODIFIED: registerShop(POST) and submitKycDocuments(/kyc-documents) removed.
 * Shop creation now happens implicitly on the first onboarding call
 * (VendorOnboardingService.getOrCreate), matching how DeliveryPartnerProfile is
 * created on first onboarding call in the Delivery module. KYC submission now
 * goes through POST /api/vendor/onboarding/submit, which requires documents to
 * actually be uploaded first (the old endpoint here auto-approved with no checks
 * at all -- see NOTES_VENDOR.md). ShopRegistrationRequestDto is now unused, delete it.
 */
@RestController
@RequestMapping("/api/vendor/shop")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorShopController {

    private final VendorShopService vendorShopService;

    @GetMapping
    public ResponseEntity<ApiResponse<ShopDto>> getShopProfile() {
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.getShopByOwner(SecurityUtils.getCurrentUserId()), "Shop profile retrieved"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ShopDto>> updateShopProfile(@Valid @RequestBody ShopUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorShopService.updateShop(SecurityUtils.getCurrentUserId(), request), "Shop updated successfully"));
    }
}
