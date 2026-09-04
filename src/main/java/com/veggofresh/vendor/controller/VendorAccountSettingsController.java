package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.VendorAccountSettingsRequestDto;
import com.veggofresh.vendor.dto.response.VendorAccountSettingsResponseDto;
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

    /**
     * Updates account settings, including the owner's personal photo, in one call. Send
     * as {@code multipart/form-data}; every field is optional (PATCH semantics) --
     * include just {@code profileImage} to change only the photo, exactly like sending
     * just {@code fullName} changes only the name. Replacing the photo auto-deletes the
     * old one from Cloudinary.
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VendorAccountSettingsResponseDto>> updateSettings(
            @Valid @ModelAttribute VendorAccountSettingsRequestDto request) {
        var settings = vendorShopService.updateAccountSettings(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Account settings updated successfully"));
    }
}
