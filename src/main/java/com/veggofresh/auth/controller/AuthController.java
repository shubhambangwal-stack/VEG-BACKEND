package com.veggofresh.auth.controller;

import com.veggofresh.auth.dto.request.LogoutRequestDto;
import com.veggofresh.auth.dto.request.OtpRequestDto;
import com.veggofresh.auth.dto.request.OtpVerifyDto;
import com.veggofresh.auth.dto.request.RefreshTokenRequestDto;
import com.veggofresh.auth.dto.response.AuthTokenResponseDto;
import com.veggofresh.auth.dto.response.UserProfileResponseDto;
import com.veggofresh.auth.service.AuthService;
import com.veggofresh.platform.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody OtpRequestDto request) {
        authService.requestOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully"));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthTokenResponseDto>> verifyOtp(@Valid @RequestBody OtpVerifyDto request) {
        AuthTokenResponseDto tokens = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(tokens, "OTP verified successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        AuthTokenResponseDto tokens = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequestDto request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getCurrentUser(@AuthenticationPrincipal String userId) {
        UserProfileResponseDto profile = authService.getCurrentUser(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success(profile, "User profile retrieved successfully"));
    }
}
