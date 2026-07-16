package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;

import java.util.UUID;

public interface CustomerProfileService {
    CustomerProfileResponseDto getOrCreateProfile(UUID userId);
    CustomerProfileResponseDto updateProfile(UUID userId);
}
