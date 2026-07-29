package com.veggofresh.delivery.service.impl;

import com.veggofresh.delivery.dto.request.BasicInfoRequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep1RequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep2RequestDto;
import com.veggofresh.delivery.dto.request.VerificationStep3RequestDto;
import com.veggofresh.delivery.dto.response.OnboardingNextAction;
import com.veggofresh.delivery.dto.response.OnboardingStatusResponseDto;
import com.veggofresh.delivery.entity.DeliveryDocumentType;
import com.veggofresh.delivery.entity.DeliveryKycStatus;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import com.veggofresh.delivery.repository.DeliveryDocumentRepository;
import com.veggofresh.delivery.repository.DeliveryPartnerProfileRepository;
import com.veggofresh.delivery.service.DeliveryOnboardingService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryOnboardingServiceImpl implements DeliveryOnboardingService {

    private final DeliveryPartnerProfileRepository profileRepository;
    private final DeliveryDocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public OnboardingStatusResponseDto getStatus(UUID userId) {
        DeliveryPartnerProfile profile = getOrCreate(userId);
        return mapToStatusDto(profile);
    }

    @Override
    public OnboardingStatusResponseDto submitBasicInfo(UUID userId, BasicInfoRequestDto request) {
        DeliveryPartnerProfile profile = getOrCreate(userId);

        profile.setFullName(request.getFullName());
        profile.setCityOfOperation(request.getCityOfOperation());
        profile.setVehicleType(request.getVehicleType());
        profile.setHasBasicInfo(true);
        profileRepository.save(profile);

        return mapToStatusDto(profile);
    }

    @Override
    public OnboardingStatusResponseDto submitVerificationStep1(UUID userId, VerificationStep1RequestDto request) {
        DeliveryPartnerProfile profile = getOrCreate(userId);
        requireBasicInfoDone(profile);
        requireDocumentUploaded(userId, DeliveryDocumentType.LICENSE, "Upload your license photo before submitting the license number");

        profile.setLicenseNumber(request.getLicenseNumber());
        profile.setVerificationStep(Math.max(profile.getVerificationStep(), 1));
        profileRepository.save(profile);

        return mapToStatusDto(profile);
    }

    @Override
    public OnboardingStatusResponseDto submitVerificationStep2(UUID userId, VerificationStep2RequestDto request) {
        DeliveryPartnerProfile profile = getOrCreate(userId);
        requireStepAtLeast(profile, 1, "Complete Step 1 (license) before Step 2");
        requireDocumentUploaded(userId, DeliveryDocumentType.INSURANCE, "Upload your insurance document before submitting vehicle details");

        profile.setPlateNumber(request.getPlateNumber());
        profile.setVehicleModel(request.getVehicleModel());
        profile.setManufactureYear(request.getManufactureYear());
        profile.setVerificationStep(Math.max(profile.getVerificationStep(), 2));
        profileRepository.save(profile);

        return mapToStatusDto(profile);
    }

    @Override
    public OnboardingStatusResponseDto submitVerificationStep3(UUID userId, VerificationStep3RequestDto request) {
        DeliveryPartnerProfile profile = getOrCreate(userId);
        requireStepAtLeast(profile, 2, "Complete Step 2 (vehicle details) before Step 3");

        profile.setBankName(request.getBankName());
        profile.setAccountHolderName(request.getAccountHolderName());
        profile.setAccountNumber(request.getAccountNumber());
        profile.setIfscCode(request.getIfscCode());
        profile.setAgreedToPayoutTerms(request.isAgreedToPayoutTerms());
        profile.setVerificationStep(3);
        // Submitting kicks off review -- kycStatus explicitly (re)set to PENDING so a
        // resubmission after REJECTED correctly re-enters the review queue.
        profile.setKycStatus(DeliveryKycStatus.PENDING);
        profileRepository.save(profile);

        return mapToStatusDto(profile);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private DeliveryPartnerProfile getOrCreate(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    DeliveryPartnerProfile newProfile = new DeliveryPartnerProfile();
                    newProfile.setUserId(userId);
                    return profileRepository.save(newProfile);
                });
    }

    private void requireBasicInfoDone(DeliveryPartnerProfile profile) {
        if (!profile.isHasBasicInfo()) {
            throw new BusinessException("ONBOARDING_BASIC_INFO_REQUIRED", "Submit basic info before starting verification", HttpStatus.BAD_REQUEST);
        }
    }

    private void requireStepAtLeast(DeliveryPartnerProfile profile, int minStep, String message) {
        if (profile.getVerificationStep() < minStep) {
            throw new BusinessException("ONBOARDING_STEP_OUT_OF_ORDER", message, HttpStatus.BAD_REQUEST);
        }
    }

    private void requireDocumentUploaded(UUID userId, DeliveryDocumentType type, String message) {
        boolean uploaded = documentRepository.findByDeliveryPartnerUserIdAndDocumentType(userId, type)
                .map(doc -> doc.getFileUrl() != null)
                .orElse(false);
        if (!uploaded) {
            throw new BusinessException("ONBOARDING_DOCUMENT_REQUIRED", message, HttpStatus.BAD_REQUEST);
        }
    }

    private OnboardingStatusResponseDto mapToStatusDto(DeliveryPartnerProfile profile) {
        boolean isSubmitted = profile.getVerificationStep() >= 3;

        OnboardingNextAction nextAction;
        if (!profile.isHasBasicInfo()) {
            nextAction = OnboardingNextAction.BASIC_INFO;
        } else if (profile.getVerificationStep() < 1) {
            nextAction = OnboardingNextAction.VERIFICATION_STEP_1;
        } else if (profile.getVerificationStep() < 2) {
            nextAction = OnboardingNextAction.VERIFICATION_STEP_2;
        } else if (profile.getVerificationStep() < 3) {
            nextAction = OnboardingNextAction.VERIFICATION_STEP_3;
        } else if (profile.getKycStatus() == DeliveryKycStatus.REJECTED) {
            nextAction = OnboardingNextAction.REJECTED;
        } else if (profile.getKycStatus() == DeliveryKycStatus.APPROVED) {
            nextAction = OnboardingNextAction.DASHBOARD;
        } else {
            nextAction = OnboardingNextAction.UNDER_REVIEW;
        }

        return OnboardingStatusResponseDto.builder()
                .hasBasicInfo(profile.isHasBasicInfo())
                .verificationStep(profile.getVerificationStep())
                .isSubmitted(isSubmitted)
                .kycStatus(profile.getKycStatus())
                .nextAction(nextAction)
                .build();
    }
}
