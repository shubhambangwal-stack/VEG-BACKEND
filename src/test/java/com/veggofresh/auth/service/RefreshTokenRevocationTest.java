package com.veggofresh.auth.service;

import com.veggofresh.auth.dto.request.RefreshTokenRequestDto;
import com.veggofresh.auth.entity.RefreshToken;
import com.veggofresh.auth.repository.OtpVerificationRepository;
import com.veggofresh.auth.repository.RefreshTokenRepository;
import com.veggofresh.auth.repository.UserRepository;
import com.veggofresh.auth.service.impl.AuthServiceImpl;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenRevocationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void refreshToken_ThrowsException_WhenTokenRevoked() {
        // Arrange
        String tokenStr = "revoked-token";
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        request.setRefreshToken(tokenStr);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(UUID.randomUUID());
        refreshToken.setToken(tokenStr);
        refreshToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        refreshToken.setRevoked(true);

        when(jwtTokenProvider.validateToken(tokenStr)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(tokenStr)).thenReturn(false);
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals("AUTH_TOKEN_REVOKED", exception.getErrorCode());
    }

    @Test
    void logout_RevokesToken_WhenValidToken() {
        // Arrange
        String tokenStr = "valid-token";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(UUID.randomUUID());
        refreshToken.setToken(tokenStr);
        refreshToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);

        when(jwtTokenProvider.validateToken(tokenStr)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(tokenStr)).thenReturn(false);
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(refreshToken));

        // Act
        authService.logout(tokenStr);

        // Assert
        verify(refreshTokenRepository, times(1)).save(argThat(RefreshToken::isRevoked));
    }
}
