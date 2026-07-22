package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.request.AddressRequestDto;
import com.veggofresh.customer.dto.response.AddressResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.service.AddressService;
import com.veggofresh.customer.service.CustomerProfileService;
import com.veggofresh.platform.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;
    private final AddressService addressService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> getProfile(@AuthenticationPrincipal String userId) {
        CustomerProfileResponseDto profile = customerProfileService.getOrCreateProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(profile, "Customer profile retrieved successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> updateProfile(@AuthenticationPrincipal String userId) {
        CustomerProfileResponseDto profile = customerProfileService.updateProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(profile, "Customer profile updated successfully"));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponseDto>>> getAddresses(@AuthenticationPrincipal String userId) {
        List<AddressResponseDto> addresses = addressService.getAddresses(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(addresses, "Addresses retrieved successfully"));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponseDto>> addAddress(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddressRequestDto request) {
        AddressResponseDto address = addressService.addAddress(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success(address, "Address added successfully"));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressResponseDto>> updateAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequestDto request) {
        AddressResponseDto address = addressService.updateAddress(UUID.fromString(userId), id, request);
        return ResponseEntity.ok(ApiResponse.success(address, "Address updated successfully"));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {
        addressService.deleteAddress(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }
}
