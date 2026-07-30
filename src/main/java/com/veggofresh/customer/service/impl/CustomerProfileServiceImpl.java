package com.veggofresh.customer.service.impl;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.customer.dto.request.CustomerProfileUpdateRequestDto;
import com.veggofresh.customer.dto.response.CustomerProfileResponseDto;
import com.veggofresh.customer.dto.response.CustomerProfileSummaryDto;
import com.veggofresh.customer.entity.CustomerProfile;
import com.veggofresh.customer.repository.AddressRepository;
import com.veggofresh.customer.repository.CustomerProfileRepository;
import com.veggofresh.customer.repository.OrderRepository;
import com.veggofresh.customer.repository.WishlistRepository;
import com.veggofresh.customer.service.CustomerProfileService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final UserLookupService userLookupService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET / CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CustomerProfileResponseDto getOrCreateProfile(UUID userId) {
        // Verify user exists in auth module
        UserSummaryDto userSummary = userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_USER_NOT_FOUND", "User not found in Auth module."));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CustomerProfile newProfile = new CustomerProfile();
                    newProfile.setUserId(userId);
                    // flush so that @CreationTimestamp / @UpdateTimestamp are populated
                    CustomerProfile saved = customerProfileRepository.saveAndFlush(newProfile);
                    return saved;
                });

        return mapToDto(profile, userSummary);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE — single API, PATCH semantics (only non-null fields are applied)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CustomerProfileResponseDto updateProfile(UUID userId, CustomerProfileUpdateRequestDto request) {
        UserSummaryDto userSummary = userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_USER_NOT_FOUND", "User not found in Auth module."));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CustomerProfile newProfile = new CustomerProfile();
                    newProfile.setUserId(userId);
                    return customerProfileRepository.saveAndFlush(newProfile);
                });

        // Apply only the fields the client actually provided
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }

        CustomerProfile saved = customerProfileRepository.saveAndFlush(profile);
        return mapToDto(saved, userSummary);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileSummaryDto getProfileSummary(UUID userId) {
        UserSummaryDto userSummary = userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_USER_NOT_FOUND", "User not found in Auth module."));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CustomerProfile newProfile = new CustomerProfile();
                    newProfile.setUserId(userId);
                    return customerProfileRepository.saveAndFlush(newProfile);
                });

        long orderCount     = orderRepository.countByUserId(userId);
        long favoritesCount = wishlistRepository.countByUserId(userId);
        long addressCount   = addressRepository.countByUserId(userId);

        // createdAt guaranteed non-null after saveAndFlush; fall back to current year if somehow null
        int memberSinceYear = safeYear(profile.getCreatedAt());

        return CustomerProfileSummaryDto.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .email(userSummary.getEmail())
                .phone(userSummary.getPhone())
                .memberSinceYear(memberSinceYear)
                .orderCount(orderCount)
                .favoritesCount(favoritesCount)
                .addressCount(addressCount)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AVATAR — kept for backward compat (called internally by old avatar upload)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CustomerProfileResponseDto updateAvatar(UUID userId, String avatarUrl) {
        CustomerProfileUpdateRequestDto req = new CustomerProfileUpdateRequestDto();
        req.setAvatarUrl(avatarUrl);
        return updateProfile(userId, req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private CustomerProfileResponseDto mapToDto(CustomerProfile profile, UserSummaryDto userSummary) {
        return CustomerProfileResponseDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .phone(userSummary.getPhone())
                .email(userSummary.getEmail())
                .memberSinceYear(safeYear(profile.getCreatedAt()))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    /**
     * Safely extracts the year from an Instant, returning the current year if the timestamp is null.
     * Guards against NPE on newly created profiles before the first flush.
     */
    private int safeYear(Instant instant) {
        if (instant == null) {
            return Instant.now().atZone(ZoneId.of("UTC")).getYear();
        }
        return instant.atZone(ZoneId.of("UTC")).getYear();
    }
}
