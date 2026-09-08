package com.veggofresh.delivery.service.impl;

import com.veggofresh.auth.dto.UserSummaryDto;
import com.veggofresh.auth.service.UserLookupService;
import com.veggofresh.delivery.dto.VendorDeliveryStatusDto;
import com.veggofresh.delivery.entity.DeliveryAssignment;
import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import com.veggofresh.delivery.entity.DeliveryOtp;
import com.veggofresh.delivery.entity.DeliveryOtpType;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import com.veggofresh.delivery.repository.DeliveryAssignmentRepository;
import com.veggofresh.delivery.repository.DeliveryOtpRepository;
import com.veggofresh.delivery.repository.DeliveryPartnerProfileRepository;
import com.veggofresh.delivery.service.DeliveryPickupInfoService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryPickupInfoServiceImpl implements DeliveryPickupInfoService {

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryOtpRepository otpRepository;
    private final DeliveryPartnerProfileRepository partnerRepository;
    private final UserLookupService userLookupService;

    @Override
    public String getPickupOtpForVendor(UUID orderId, UUID shopOwnerUserId) {
        DeliveryAssignment assignment = assignmentRepository
                .findByOrderIdAndStatusIn(orderId, List.of(DeliveryAssignmentStatus.ACCEPTED, DeliveryAssignmentStatus.ARRIVED_AT_STORE))
                .orElse(null);

        if (assignment == null) {
            return null; // no partner has accepted yet -- nothing to hand over
        }

        requireOwnedByShop(assignment, shopOwnerUserId);

        return otpRepository.findByAssignmentIdAndType(assignment.getId(), DeliveryOtpType.PICKUP)
                .filter(otp -> !otp.isVerified())
                .map(DeliveryOtp::getOtpCode)
                .orElse(null);
    }

    @Override
    public VendorDeliveryStatusDto getDeliveryStatusForVendor(UUID orderId, UUID shopOwnerUserId) {
        List<DeliveryAssignment> all = assignmentRepository.findByOrderId(orderId);
        if (all.isEmpty()) {
            return VendorDeliveryStatusDto.builder().dispatched(false).build();
        }

        DeliveryAssignment latest = all.stream()
                .max(Comparator.comparing(DeliveryAssignment::getCreatedAt))
                .orElseThrow();

        requireOwnedByShop(latest, shopOwnerUserId);

        String partnerName = null;
        String partnerPhone = null;
        if (latest.getDeliveryPartnerUserId() != null) {
            partnerName = partnerRepository.findByUserId(latest.getDeliveryPartnerUserId())
                    .map(DeliveryPartnerProfile::getFullName).orElse(null);
            partnerPhone = userLookupService.findById(latest.getDeliveryPartnerUserId())
                    .map(UserSummaryDto::getPhone).orElse(null);
        }

        return VendorDeliveryStatusDto.builder()
                .dispatched(true)
                .status(latest.getStatus().name())
                .partnerName(partnerName)
                .partnerPhone(partnerPhone)
                .build();
    }

    private void requireOwnedByShop(DeliveryAssignment assignment, UUID shopOwnerUserId) {
        if (!shopOwnerUserId.equals(assignment.getShopOwnerUserId())) {
            throw new BusinessException("DELIVERY_ASSIGNMENT_NOT_OWNED", "This order does not belong to your shop", HttpStatus.FORBIDDEN);
        }
    }
}
