package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.CustomerProfileUpdateRequestDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileSummaryDto;

import java.util.UUID;

public interface CustomerProfileService {
    CustomerProfileResponseDto getOrCreateProfile(UUID userId);

    /** GAP 16 — FIX: actually save fullName (was a stub before) */
    CustomerProfileResponseDto updateProfile(UUID userId, CustomerProfileUpdateRequestDto request);

    /** GAP 16 — profile + order/address/favorites counts in one call */
    CustomerProfileSummaryDto getProfileSummary(UUID userId);

    /** GAP 16 — save avatar URL */
    CustomerProfileResponseDto updateAvatar(UUID userId, String avatarUrl);
}
