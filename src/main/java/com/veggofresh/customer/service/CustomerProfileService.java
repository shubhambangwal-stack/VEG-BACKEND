package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.CustomerProfileUpdateRequestDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileSummaryDto;

import java.util.UUID;

public interface CustomerProfileService {
    CustomerProfileResponseDto getOrCreateProfile(UUID userId);
    CustomerProfileResponseDto updateProfile(UUID userId, CustomerProfileUpdateRequestDto request);
    CustomerProfileSummaryDto getProfileSummary(UUID userId);
    CustomerProfileResponseDto updateAvatar(UUID userId, String avatarUrl);
}
