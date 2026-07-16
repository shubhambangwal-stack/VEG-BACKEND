package com.veggofresh.admin.controller;

import com.veggofresh.admin.dto.request.AdminLoginRequestDto;
import com.veggofresh.admin.service.AdminAuthService;
import com.veggofresh.auth.dto.response.AuthTokenResponseDto;
import com.veggofresh.platform.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponseDto>> login(@Valid @RequestBody AdminLoginRequestDto request) {
        AuthTokenResponseDto tokens = adminAuthService.loginAdmin(request);
        return ResponseEntity.ok(ApiResponse.success(tokens, "Admin logged in successfully"));
    }
}
