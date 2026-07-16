package com.veggofresh.admin.service;

import com.veggofresh.admin.dto.request.AdminLoginRequestDto;
import com.veggofresh.auth.dto.response.AuthTokenResponseDto;

public interface AdminAuthService {
    AuthTokenResponseDto loginAdmin(AdminLoginRequestDto request);
}
