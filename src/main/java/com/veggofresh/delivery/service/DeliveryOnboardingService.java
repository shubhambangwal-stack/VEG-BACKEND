package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.request.BasicInfoRequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep1RequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep2RequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep3RequestDto;
import com.veggofresh.delivery.dto.response.OnboardingStatusResponseDto;

import java.util.UUID;

public interface DeliveryOnboardingService {
    /** Call right after OTP login succeeds -- tells the app exactly which screen to show next. */
    OnboardingStatusResponseDto getStatus(UUID userId);

    OnboardingStatusResponseDto submitBasicInfo(UUID userId, BasicInfoRequestDto request);
    OnboardingStatusResponseDto submitVerificationStep1(UUID userId, VerificationStep1RequestDto request);
    OnboardingStatusResponseDto submitVerificationStep2(UUID userId, VerificationStep2RequestDto request);
    OnboardingStatusResponseDto submitVerificationStep3(UUID userId, VerificationStep3RequestDto request);
}
