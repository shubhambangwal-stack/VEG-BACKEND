package com.veggofresh.delivery.service.impl;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.delivery.dto.DeliveryKycReviewDto;
import com.veggofresh.delivery.entity.DeliveryKycStatus;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import com.veggofresh.delivery.repository.DeliveryPartnerProfileRepository;
import com.veggofresh.delivery.service.DeliveryKycService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Real implementation -- resolves the blocking dependency Admin's
 * AdminDeliveryKycController was built against last round. See that controller's
 * javadoc and Admin's NOTES_ADMIN.md for the original contract this fulfills.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryKycServiceImpl implements DeliveryKycService {

    private static final int SUBMITTED_STEP = 3;

    private final DeliveryPartnerProfileRepository partnerRepository;
    private final UserLookupService userLookupService;

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryKycReviewDto> listPendingKyc(Pageable pageable) {
        return partnerRepository.findByKycStatusAndVerificationStep(DeliveryKycStatus.PENDING, SUBMITTED_STEP, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryKycReviewDto getKycDetail(UUID userId) {
        DeliveryPartnerProfile partner = getPartner(userId);
        return mapToDto(partner);
    }

    @Override
    public void approveKyc(UUID userId) {
        DeliveryPartnerProfile partner = getPartner(userId);
        partner.setKycStatus(DeliveryKycStatus.APPROVED);
        partner.setRejectionReason(null);
        partnerRepository.save(partner);
    }

    @Override
    public void rejectKyc(UUID userId, String reason) {
        DeliveryPartnerProfile partner = getPartner(userId);
        partner.setKycStatus(DeliveryKycStatus.REJECTED);
        partner.setRejectionReason(reason);
        partnerRepository.save(partner);
    }

    private DeliveryPartnerProfile getPartner(UUID userId) {
        return partnerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));
    }

    private DeliveryKycReviewDto mapToDto(DeliveryPartnerProfile p) {
        String phone = userLookupService.findById(p.getUserId()).map(UserSummaryDto::getPhone).orElse(null);
        return DeliveryKycReviewDto.builder()
                .userId(p.getUserId())
                .fullName(p.getFullName())
                .phone(phone)
                .vehicleType(p.getVehicleType())
                .vehicleModel(p.getVehicleModel())
                .plateNumber(p.getPlateNumber())
                .licenseNumber(p.getLicenseNumber())
                .kycStatus(p.getKycStatus().name())
                .rejectionReason(p.getRejectionReason())
                .submittedAt(p.getUpdatedAt())
                .build();
    }
}
