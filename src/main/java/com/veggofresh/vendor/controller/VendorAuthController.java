package com.veggofresh.vendor.controller;

import com.veggofresh.platform.common.ApiResponse;
import com.veggofresh.vendor.dto.request.VendorLoginRequestDto;
import com.veggofresh.vendor.dto.request.VendorRegisterRequestDto;
import com.veggofresh.vendor.dto.response.VendorAuthResponseDto;
import com.veggofresh.vendor.service.VendorAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/auth")
@RequiredArgsConstructor
public class VendorAuthController {

    private final VendorAuthService vendorAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<VendorAuthResponseDto>> login(@Valid @RequestBody VendorLoginRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorAuthService.login(request), "Login successful"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<VendorAuthResponseDto>> register(@Valid @RequestBody VendorRegisterRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(vendorAuthService.register(request), "Registration successful"));
    }
}
