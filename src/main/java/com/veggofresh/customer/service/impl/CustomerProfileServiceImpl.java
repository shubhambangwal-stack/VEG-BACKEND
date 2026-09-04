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
import com.veggofresh.platform.storage.CloudinaryService;
import com.veggofresh.platform.storage.CloudinaryUploadResult;
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
    private final CloudinaryService cloudinaryService;

    @Override
    public CustomerProfileResponseDto getOrCreateProfile(UUID userId) {
        UserSummaryDto userSummary = requireUser(userId);
        CustomerProfile profile = getOrCreateEntity(userId);
        return mapToDto(profile, userSummary);
    }

    @Override
    public CustomerProfileResponseDto updateProfile(UUID userId, CustomerProfileUpdateRequestDto request) {
        UserSummaryDto userSummary = requireUser(userId);
        CustomerProfile profile = getOrCreateEntity(userId);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            profile.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            profile.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            // Upload the new image first -- only swap over and delete the old one once
            // the new upload has actually succeeded.
            CloudinaryUploadResult upload = cloudinaryService.uploadImage(
                    request.getAvatar(), "veggofresh/customers/" + userId + "/avatar");
            String oldAvatarPublicId = profile.getAvatarPublicId();
            profile.setAvatarUrl(upload.url());
            profile.setAvatarPublicId(upload.publicId());
            cloudinaryService.deleteQuietly(oldAvatarPublicId);
        }

        CustomerProfile saved = customerProfileRepository.saveAndFlush(profile);
        return mapToDto(saved, userSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileSummaryDto getProfileSummary(UUID userId) {
        requireUser(userId);
        CustomerProfile profile = getOrCreateEntity(userId);

        long orderCount     = orderRepository.countByUserId(userId);
        long favoritesCount = wishlistRepository.countByUserId(userId);
        long addressCount   = addressRepository.countByUserId(userId);

        return CustomerProfileSummaryDto.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .email(profile.getEmail())
                .phone(userLookupService.findById(userId)
                        .map(UserSummaryDto::getPhone)
                        .orElse(null))
                .memberSinceYear(safeYear(profile.getCreatedAt()))
                .orderCount(orderCount)
                .favoritesCount(favoritesCount)
                .addressCount(addressCount)
                .build();
    }

    @Override
    public CustomerProfileResponseDto submitBasicInfo(UUID userId, String fullName) {
        UserSummaryDto userSummary = requireUser(userId);
        CustomerProfile profile = getOrCreateEntity(userId);

        profile.setFullName(fullName);

        CustomerProfile saved = customerProfileRepository.saveAndFlush(profile);
        return mapToDto(saved, userSummary);
    }

    // -------------------------------------------------------------------------

    private UserSummaryDto requireUser(UUID userId) {
        return userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_USER_NOT_FOUND", "User not found in Auth module."));
    }

    private CustomerProfile getOrCreateEntity(UUID userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CustomerProfile newProfile = new CustomerProfile();
                    newProfile.setUserId(userId);
                    return customerProfileRepository.saveAndFlush(newProfile);
                });
    }

    private CustomerProfileResponseDto mapToDto(CustomerProfile profile, UserSummaryDto userSummary) {
        return CustomerProfileResponseDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .phone(userSummary.getPhone())
                .email(profile.getEmail())
                .memberSinceYear(safeYear(profile.getCreatedAt()))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private int safeYear(Instant instant) {
        if (instant == null) {
            return Instant.now().atZone(ZoneId.of("UTC")).getYear();
        }
        return instant.atZone(ZoneId.of("UTC")).getYear();
    }
}
