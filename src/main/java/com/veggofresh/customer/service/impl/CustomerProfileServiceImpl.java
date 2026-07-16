package com.veggofresh.customer.service.impl;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.entity.CustomerProfile;
import com.veggofresh.customer.repository.CustomerProfileRepository;
import com.veggofresh.customer.service.CustomerProfileService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserLookupService userLookupService;

    @Override
    public CustomerProfileResponseDto getOrCreateProfile(UUID userId) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Verify that the user exists in auth module
                    UserSummaryDto userSummary = userLookupService.findById(userId)
                            .orElseThrow(() -> new BusinessException("CUSTOMER_USER_NOT_FOUND", "User not found in Auth module."));
                    
                    CustomerProfile newProfile = new CustomerProfile();
                    newProfile.setUserId(userId);
                    return customerProfileRepository.save(newProfile);
                });

        UserSummaryDto userSummary = userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_USER_NOT_FOUND", "User not found in Auth module."));

        return mapToDto(profile, userSummary);
    }

    @Override
    public CustomerProfileResponseDto updateProfile(UUID userId) {
        // Customer profile only contains userId as reference.
        // If there were other profile fields, we'd update them here.
        // For now, we fetch/ensure existence and return the profile.
        return getOrCreateProfile(userId);
    }

    private CustomerProfileResponseDto mapToDto(CustomerProfile profile, UserSummaryDto userSummary) {
        return CustomerProfileResponseDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .phone(userSummary.getPhone())
                .email(userSummary.getEmail())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
