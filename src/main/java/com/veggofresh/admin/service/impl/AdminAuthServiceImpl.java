package com.veggofresh.admin.service.impl;

import com.veggofresh.admin.dto.request.AdminLoginRequestDto;
import com.veggofresh.admin.service.AdminAuthService;
import com.veggofresh.auth.dto.response.AuthTokenResponseDto;
import com.veggofresh.auth.entity.RefreshToken;
import com.veggofresh.auth.entity.User;
import com.veggofresh.auth.entity.UserRole;
import com.veggofresh.auth.repository.RefreshTokenRepository;
import com.veggofresh.auth.repository.UserRepository;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthTokenResponseDto loginAdmin(AdminLoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("ADMIN_AUTH_FAILED", "Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException("ADMIN_ACCESS_DENIED", "Access denied. Only administrators are allowed.", HttpStatus.FORBIDDEN);
        }

        if (user.isBlocked()) {
            throw new BusinessException("ADMIN_USER_BLOCKED", "User account is blocked", HttpStatus.FORBIDDEN);
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("ADMIN_AUTH_FAILED", "Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        return generateTokens(user);
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
