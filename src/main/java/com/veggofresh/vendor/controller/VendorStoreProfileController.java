package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.StoreProfileRequestDto;
import com.veggofresh.vendor.dto.response.StoreProfileResponseDto;
import com.veggofresh.vendor.service.VendorShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
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

    /**
     * Updates the store profile, including the store photo, in one call. Send as
     * {@code multipart/form-data}; include the {@code storeImage} file part to replace
     * the current store photo (auto-deletes the old one from Cloudinary), or omit it to
     * leave the existing photo unchanged. All other fields keep their previous
     * required/optional behavior.
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoreProfileResponseDto>> updateStoreProfile(
            @Valid @ModelAttribute StoreProfileRequestDto request) {
        var profile = vendorShopService.updateStoreProfile(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Store profile updated successfully"));
    }
}
