package com.veggofresh.delivery.service.impl;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.delivery.dto.request.DeliveryLocationStatusRequestDto;
import com.veggofresh.delivery.dto.request.DeliveryProfileRequestDto;
import com.veggofresh.delivery.dto.response.DeliveryProfileResponseDto;
import com.veggofresh.delivery.entity.DeliveryKycStatus;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import com.veggofresh.delivery.repository.DeliveryPartnerProfileRepository;
import com.veggofresh.delivery.service.DeliveryProfileService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryProfileServiceImpl implements DeliveryProfileService {

    private final DeliveryPartnerProfileRepository profileRepository;
    private final UserLookupService userLookupService;

    @Override
    public DeliveryProfileResponseDto getOrCreateProfile(UUID userId) {
        UserSummaryDto user = userLookupService.findById(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_USER_NOT_FOUND", "User not found in Auth module", HttpStatus.NOT_FOUND));

        DeliveryPartnerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    DeliveryPartnerProfile newProfile = new DeliveryPartnerProfile();
                    newProfile.setUserId(userId);
                    return profileRepository.save(newProfile);
                });

        return mapToDto(profile, user.getPhone());
    }

    @Override
    public DeliveryProfileResponseDto updateProfile(UUID userId, DeliveryProfileRequestDto request) {
        DeliveryPartnerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        profile.setVehicleType(request.getVehicleType());
        profileRepository.save(profile);

        UserSummaryDto user = userLookupService.findById(userId).orElseThrow();
        return mapToDto(profile, user.getPhone());
    }

    @Override
    public DeliveryProfileResponseDto updateStatus(UUID userId, DeliveryLocationStatusRequestDto request) {
        DeliveryPartnerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(request.getOnline()) && profile.getKycStatus() != DeliveryKycStatus.APPROVED) {
            throw new BusinessException("DELIVERY_KYC_NOT_APPROVED", "Cannot go online until KYC is approved", HttpStatus.FORBIDDEN);
        }

        profile.setOnline(Boolean.TRUE.equals(request.getOnline()));
        if (request.getCurrentLatitude() != null) profile.setCurrentLatitude(request.getCurrentLatitude());
        if (request.getCurrentLongitude() != null) profile.setCurrentLongitude(request.getCurrentLongitude());
        profileRepository.save(profile);

        UserSummaryDto user = userLookupService.findById(userId).orElseThrow();
        return mapToDto(profile, user.getPhone());
    }

    @Override
    public DeliveryProfileResponseDto submitKycDocuments(UUID userId) {
        // Document upload/storage is out of scope for this pass — mocked as moving
        // the status back to PENDING for (re-)review. Wire real file storage later.
        DeliveryPartnerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        profile.setKycStatus(DeliveryKycStatus.PENDING);
        profileRepository.save(profile);

        UserSummaryDto user = userLookupService.findById(userId).orElseThrow();
        return mapToDto(profile, user.getPhone());
    }


    @Override
    public DeliveryProfileResponseDto approveKycForTesting(UUID userId) {
        DeliveryPartnerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        profile.setKycStatus(DeliveryKycStatus.APPROVED);
        profileRepository.save(profile);

        UserSummaryDto user = userLookupService.findById(userId).orElseThrow();
        return mapToDto(profile, user.getPhone());
    }

    private DeliveryProfileResponseDto mapToDto(DeliveryPartnerProfile profile, String phone) {
        return DeliveryProfileResponseDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .phone(phone)
                .kycStatus(profile.getKycStatus())
                .online(profile.isOnline())
                .currentLatitude(profile.getCurrentLatitude())
                .currentLongitude(profile.getCurrentLongitude())
                .vehicleType(profile.getVehicleType())
                .build();
    }
}
