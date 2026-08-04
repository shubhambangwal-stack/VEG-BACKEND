package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import com.veggofresh.vendor.dto.request.OperatingHourUpdateRequestDto;
import com.veggofresh.vendor.dto.request.SpecialClosureRequestDto;
import com.veggofresh.vendor.dto.response.OperatingHoursSummaryResponseDto;
import com.veggofresh.vendor.dto.response.SpecialClosureResponseDto;
import com.veggofresh.vendor.service.VendorOperatingHoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/operating-hours")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorOperatingHoursController {

    private final VendorOperatingHoursService vendorOperatingHoursService;

    @GetMapping
    public ResponseEntity<ApiResponse<OperatingHoursSummaryResponseDto>> getOperatingHours() {
        var hours = vendorOperatingHoursService.getOperatingHours(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(hours, "Operating hours retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<OperatingHoursSummaryResponseDto>> updateOperatingHours(
            @Valid @RequestBody List<OperatingHourUpdateRequestDto> updates) {
        var hours = vendorOperatingHoursService.updateOperatingHours(SecurityUtils.getCurrentUserId(), updates);
        return ResponseEntity.ok(ApiResponse.success(hours, "Operating hours updated successfully"));
    }

    @PostMapping("/closures")
    public ResponseEntity<ApiResponse<SpecialClosureResponseDto>> addClosure(
            @Valid @RequestBody SpecialClosureRequestDto request) {
        var closure = vendorOperatingHoursService.addSpecialClosure(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(closure, "Special closure added successfully"));
    }

    @DeleteMapping("/closures/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClosure(@PathVariable UUID id) {
        vendorOperatingHoursService.deleteSpecialClosure(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Special closure removed successfully"));
    }
}
