package com.veggofresh.auth.service.impl;

import com.veggofresh.auth.dto.request.OtpRequestDto;
import com.veggofresh.auth.dto.request.OtpVerifyDto;
import com.veggofresh.auth.dto.request.RefreshTokenRequestDto;
import com.veggofresh.auth.dto.response.AuthTokenResponseDto;
import com.veggofresh.auth.dto.response.UserProfileResponseDto;
import com.veggofresh.auth.entity.OtpVerification;
import com.veggofresh.auth.entity.RefreshToken;
import com.veggofresh.auth.entity.User;
import com.veggofresh.auth.repository.OtpVerificationRepository;
import com.veggofresh.auth.repository.RefreshTokenRepository;
import com.veggofresh.auth.repository.UserRepository;
import com.veggofresh.auth.service.AuthService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_RATE_LIMIT_SECONDS = 60;

    @Override
    public void requestOtp(OtpRequestDto request) {
        String phone = request.getPhone();

        // Rate limiting check
        List<OtpVerification> recentOtps = otpVerificationRepository
                .findByPhoneAndCreatedAtAfter(phone, Instant.now().minus(OTP_RATE_LIMIT_SECONDS, ChronoUnit.SECONDS));
        
        if (!recentOtps.isEmpty()) {
            throw new BusinessException("AUTH_OTP_RATE_LIMITED", "Please wait before requesting another OTP", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Generate OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        OtpVerification verification = new OtpVerification();
        verification.setPhone(phone);
        verification.setOtpCode(otpCode);
        verification.setExpiresAt(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        otpVerificationRepository.save(verification);

        // Mock SMS sending
        log.info("MOCK SMS -> Sending OTP {} to phone {}", otpCode, phone);
    }

    @Override
    public AuthTokenResponseDto verifyOtp(OtpVerifyDto request) {
        String phone = request.getPhone();
        
        OtpVerification verification = otpVerificationRepository
                .findTopByPhoneOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new BusinessException("AUTH_OTP_NOT_FOUND", "No OTP found for this phone"));

        if (verification.isVerified()) {
            throw new BusinessException("AUTH_OTP_ALREADY_VERIFIED", "This OTP has already been verified");
        }

        if (Instant.now().isAfter(verification.getExpiresAt())) {
            throw new BusinessException("AUTH_OTP_EXPIRED", "OTP has expired");
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (verification.getAttempts() > MAX_OTP_ATTEMPTS) {
            otpVerificationRepository.save(verification);
            throw new BusinessException("AUTH_OTP_MAX_ATTEMPTS", "Maximum OTP attempts exceeded");
        }

        if (!verification.getOtpCode().equals(request.getOtp())) {
            otpVerificationRepository.save(verification);
            throw new BusinessException("AUTH_OTP_INVALID", "Invalid OTP code", HttpStatus.UNAUTHORIZED);
        }

        verification.setVerified(true);
        otpVerificationRepository.save(verification);

        // Find or create user
        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            User newUser = new User();
            newUser.setPhone(phone);
            newUser.setRole(request.getRole());
            newUser.setVerified(true);
            return userRepository.save(newUser);
        });

        if (user.isBlocked()) {
            throw new BusinessException("AUTH_USER_BLOCKED", "User account is blocked", HttpStatus.FORBIDDEN);
        }
        
        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }

        return generateTokens(user);
    }

    @Override
    public AuthTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        String tokenStr = request.getRefreshToken();
        
        if (!jwtTokenProvider.validateToken(tokenStr)) {
            throw new BusinessException("AUTH_TOKEN_INVALID", "Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }
        
        if (jwtTokenProvider.isAccessToken(tokenStr)) {
            throw new BusinessException("AUTH_TOKEN_TYPE_INVALID", "Expected a refresh token", HttpStatus.BAD_REQUEST);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BusinessException("AUTH_TOKEN_NOT_FOUND", "Refresh token not found", HttpStatus.UNAUTHORIZED));

        if (refreshToken.isRevoked()) {
            throw new BusinessException("AUTH_TOKEN_REVOKED", "Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }

        if (Instant.now().isAfter(refreshToken.getExpiresAt())) {
            throw new BusinessException("AUTH_TOKEN_EXPIRED", "Refresh token has expired", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BusinessException("AUTH_USER_NOT_FOUND", "User not found"));

        if (user.isBlocked()) {
            throw new BusinessException("AUTH_USER_BLOCKED", "User account is blocked", HttpStatus.FORBIDDEN);
        }

        // Rotate refresh token for better security
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return generateTokens(user);
    }

    @Override
    public void logout(String refreshTokenStr) {
        if (!jwtTokenProvider.validateToken(refreshTokenStr) || jwtTokenProvider.isAccessToken(refreshTokenStr)) {
            // Ignore invalid tokens on logout
            return;
        }
        
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("AUTH_USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        return UserProfileResponseDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .isBlocked(user.isBlocked())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthTokenResponseDto generateTokens(User user) {
        String email = user.getEmail() != null ? user.getEmail() : "";
        String accessTokenStr = jwtTokenProvider.generateAccessToken(user.getId(), email, user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId(), email, user.getRole().name());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiresAt(jwtTokenProvider.extractExpiry(refreshTokenStr));
        refreshTokenRepository.save(refreshToken);

        return AuthTokenResponseDto.builder()
                .accessToken(accessTokenStr)
                .refreshToken(refreshTokenStr)
                .build();
    }
}
