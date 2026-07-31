package com.veggofresh.delivery.service;

import com.veggofresh.delivery.dto.request.BasicInfoRequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep3RequestDto;
import com.veggofresh.delivery.dto.response.OnboardingStatusResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DeliveryOnboardingService {
    /** Call right after OTP login succeeds -- tells the app exactly which screen to show next. */
    OnboardingStatusResponseDto getStatus(UUID userId);

    OnboardingStatusResponseDto submitBasicInfo(UUID userId, BasicInfoRequestDto request);

    /** Combined: license photo + license number, submitted together in one call (matches the mockup screen). */
    OnboardingStatusResponseDto submitVerificationStep1(UUID userId, String licenseNumber, MultipartFile licensePhoto);

    /** Combined: insurance document + plate/model/year, submitted together in one call. */
    OnboardingStatusResponseDto submitVerificationStep2(UUID userId, String plateNumber, String vehicleModel,
                                                          Integer manufactureYear, MultipartFile insurancePhoto);

    OnboardingStatusResponseDto submitVerificationStep3(UUID userId, VerificationStep3RequestDto request);
}
