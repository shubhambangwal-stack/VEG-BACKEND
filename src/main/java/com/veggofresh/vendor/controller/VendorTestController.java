package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.entity.KycStatus;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.repository.ShopRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEST-ONLY CONTROLLER — NOT PART OF THE MODULE SPEC.
 *
 * Stands in for the real Admin-module KYC approval/rejection endpoints, which don't
 * exist yet. Also replaces the bug found in the original VendorShopService.submitKycDocuments()
 * (which auto-approved KYC synchronously inside the real submit flow) -- that logic
 * now lives here instead, clearly marked as fake.
 *
 * DELETE THIS FILE once Admin module's real KYC review endpoints exist.
 */
@RestController
@RequestMapping("/api/vendor/test")
@RequiredArgsConstructor
public class VendorTestController {

    private final ShopRepository shopRepository;

    @PostMapping("/approve-kyc")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> approveKyc() {
        Shop shop = requireShop();
        shop.setKycStatus(KycStatus.APPROVED);
        shop.setKycRejectionReason(null);
        shopRepository.save(shop);
        return ResponseEntity.ok(ApiResponse.success("[TEST-ONLY] KYC approved"));
    }

    @PostMapping("/reject-kyc")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> rejectKyc(
            @RequestParam(defaultValue = "Your provided Business License appears to be expired.") String reason) {
        Shop shop = requireShop();
        shop.setKycStatus(KycStatus.REJECTED);
        shop.setKycRejectionReason(reason);
        shopRepository.save(shop);
        return ResponseEntity.ok(ApiResponse.success("[TEST-ONLY] KYC rejected"));
    }

    private Shop requireShop() {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Vendor shop not found", HttpStatus.NOT_FOUND));
    }
}
