package com.veggofresh.admin.controller;

import com.veggofresh.admin.dto.request.PlatformSettingsUpdateRequestDto;
import com.veggofresh.admin.dto.response.PlatformSettingsResponseDto;
import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.platform.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-wide configuration: delivery radius (shared by customer<->vendor AND
 * vendor<->delivery-partner matching), platform commission %, accept timeouts, and
 * re-broadcast bounds. Single settings row, PUT (not PATCH) semantics -- see
 * PlatformSettingsUpdateRequestDto javadoc for why partial updates aren't supported here.
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<PlatformSettingsResponseDto>> getSettings() {
        PlatformSettingsResponseDto settings = platformSettingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings, "Platform settings retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PlatformSettingsResponseDto>> updateSettings(
            @Valid @RequestBody PlatformSettingsUpdateRequestDto request) {
        PlatformSettingsResponseDto settings = platformSettingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Platform settings updated successfully"));
    }
}
