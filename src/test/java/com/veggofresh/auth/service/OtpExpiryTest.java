package com.veggofresh.auth.service;

import com.veggofresh.auth.dto.request.OtpVerifyDto;
import com.veggofresh.auth.entity.OtpVerification;
import com.veggofresh.auth.entity.UserRole;
import com.veggofresh.auth.repository.OtpVerificationRepository;
import com.veggofresh.auth.repository.RefreshTokenRepository;
import com.veggofresh.auth.repository.UserRepository;
import com.veggofresh.auth.service.impl.AuthServiceImpl;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OtpExpiryTest {

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

    private OtpVerification verification;

    @BeforeEach
    void setUp() {
        verification = new OtpVerification();
        verification.setPhone("+1234567890");
        verification.setOtpCode("123456");
        verification.setVerified(false);
        verification.setAttempts(0);
    }

    @Test
    void verifyOtp_ThrowsException_WhenOtpExpired() {
        // Arrange
        verification.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(otpVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(anyString()))
                .thenReturn(Optional.of(verification));

        OtpVerifyDto verifyDto = new OtpVerifyDto();
        verifyDto.setPhone("+1234567890");
        verifyDto.setOtp("123456");
        verifyDto.setRole(UserRole.CUSTOMER);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.verifyOtp(verifyDto));
        assertEquals("AUTH_OTP_EXPIRED", exception.getErrorCode());
    }
}
