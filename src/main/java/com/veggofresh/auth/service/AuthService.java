package com.veggofresh.auth.service;

import com.veggofresh.auth.dto.request.OtpRequestDto;
import com.veggofresh.auth.dto.request.OtpVerifyDto;
import com.veggofresh.auth.dto.request.RefreshTokenRequestDto;
import com.veggofresh.auth.dto.response.AuthTokenResponseDto;
import com.veggofresh.auth.dto.response.UserProfileResponseDto;

import java.util.UUID;

public interface AuthService {
    void requestOtp(OtpRequestDto request);
    AuthTokenResponseDto verifyOtp(OtpVerifyDto request);
    AuthTokenResponseDto refreshToken(RefreshTokenRequestDto request);
    void logout(String refreshToken);
    UserProfileResponseDto getCurrentUser(UUID userId);
}
