package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.CustomerProfileUpdateRequestDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileSummaryDto;

import java.util.UUID;

public interface CustomerProfileService {
    CustomerProfileResponseDto getOrCreateProfile(UUID userId);

    /** Full profile update: fullName, email, and/or avatar (multipart), all optional, PATCH semantics. */
    CustomerProfileResponseDto updateProfile(UUID userId, CustomerProfileUpdateRequestDto request);

    CustomerProfileSummaryDto getProfileSummary(UUID userId);

    /**
     * One-time onboarding step, right after OTP verification: sets the customer's
     * fullName (required). Called from {@code PUT /api/customer/onboarding/basic-info}.
     */
    CustomerProfileResponseDto submitBasicInfo(UUID userId, String fullName);
}
