package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.request.AddressRequestDto;
import com.veggofresh.customer.dto.request.CustomerProfileUpdateRequestDto;
import com.veggofresh.customer.dto.response.AddressResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileSummaryDto;
import com.veggofresh.customer.service.AddressService;
import com.veggofresh.customer.service.CustomerProfileService;
import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

/**
 * Customer Profile & Address Management
 *
 * <pre>
 * GET  /api/customer/profile          — fetch (or auto-create) profile
 * PUT  /api/customer/profile          — update fullName and/or avatarUrl (PATCH semantics)
 * GET  /api/customer/profile/summary  — aggregated stats card
 *
 * GET    /api/customer/addresses      — list addresses
 * POST   /api/customer/addresses      — add address
 * PUT    /api/customer/addresses/{id} — update address
 * DELETE /api/customer/addresses/{id} — soft-delete address
 * </pre>
 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;
    private final AddressService addressService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> getProfile() {
        CustomerProfileResponseDto profile =
                customerProfileService.getOrCreateProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(profile, "Customer profile retrieved successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> updateProfile(
            @Valid @RequestBody CustomerProfileUpdateRequestDto request) {
        CustomerProfileResponseDto profile =
                customerProfileService.updateProfile(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Customer profile updated successfully"));
    }

    @GetMapping("/profile/summary")
    public ResponseEntity<ApiResponse<CustomerProfileSummaryDto>> getProfileSummary() {
        CustomerProfileSummaryDto summary =
                customerProfileService.getProfileSummary(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(summary, "Customer profile summary retrieved successfully"));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponseDto>>> getAddresses() {
        List<AddressResponseDto> addresses = addressService.getAddresses(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(addresses, "Addresses retrieved successfully"));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponseDto>> addAddress(
            @Valid @RequestBody AddressRequestDto request) {
        AddressResponseDto address = addressService.addAddress(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(address, "Address added successfully"));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressResponseDto>> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequestDto request) {
        AddressResponseDto address = addressService.updateAddress(SecurityUtils.getCurrentUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(address, "Address updated successfully"));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }
}
