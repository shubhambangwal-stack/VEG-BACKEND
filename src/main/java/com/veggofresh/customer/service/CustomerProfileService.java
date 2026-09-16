package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.CustomerProfileUpdateRequestDto;
import com.veggofresh.customer.dto.response.CustomerOnboardingStatusResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileSummaryDto;

import java.util.UUID;

public interface CustomerProfileService {
    CustomerProfileResponseDto getOrCreateProfile(UUID userId);

    /**
     * Mirrors Delivery/Vendor's onboarding status pattern. Called from
     * {@code GET /api/customer/onboarding/status} right after OTP verification
     * (and again on app relaunch) so routing to the basic-info screen vs. home
     * is decided server-side instead of the client inferring it from a blank
     * fullName on the profile response.
     */
    CustomerOnboardingStatusResponseDto getOnboardingStatus(UUID userId);

    /** Full profile update: fullName, email, and/or avatar (multipart), all optional, PATCH semantics. */
    CustomerProfileResponseDto updateProfile(UUID userId, CustomerProfileUpdateRequestDto request);

    CustomerProfileSummaryDto getProfileSummary(UUID userId);

    /**
     * One-time onboarding step, right after OTP verification: sets the customer's
     * fullName (required). Called from {@code PUT /api/customer/onboarding/basic-info}.
     */
    CustomerProfileResponseDto submitBasicInfo(UUID userId, String fullName);
}
