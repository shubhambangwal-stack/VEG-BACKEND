package com.veggofresh.delivery.service.impl;

import com.veggofresh.customer.service.CustomerOrderService;
import com.veggofresh.delivery.dto.response.DeliveryAssignmentResponseDto;
import com.veggofresh.delivery.entity.DeliveryAssignment;
import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import com.veggofresh.delivery.entity.DeliveryKycStatus;
import com.veggofresh.delivery.entity.DeliveryOtp;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import com.veggofresh.delivery.entity.EarningRecord;
import com.veggofresh.delivery.repository.DeliveryAssignmentRepository;
import com.veggofresh.delivery.repository.DeliveryOtpRepository;
import com.veggofresh.delivery.repository.DeliveryPartnerProfileRepository;
import com.veggofresh.delivery.repository.EarningRecordRepository;
import com.veggofresh.delivery.service.DeliveryAssignmentService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryAssignmentServiceImpl implements DeliveryAssignmentService {

    private static final int ASSIGNMENT_EXPIRY_SECONDS = 60;
    private static final double DEFAULT_SEARCH_RADIUS_KM = 5.0;
    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final int MAX_OTP_ATTEMPTS = 5;
    // Flat rate per delivery for now — no fare/distance-based earning engine exists yet.
    private static final BigDecimal FLAT_EARNING_PER_DELIVERY = BigDecimal.valueOf(40);

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryPartnerProfileRepository partnerRepository;
    private final DeliveryOtpRepository otpRepository;
    private final EarningRecordRepository earningRecordRepository;
    private final CustomerOrderService customerOrderService;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentResponseDto> getNearbyAssignments(UUID deliveryPartnerUserId, double lat, double lng, double radiusKm) {
        double radius = radiusKm > 0 ? radiusKm : DEFAULT_SEARCH_RADIUS_KM;
        return assignmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == DeliveryAssignmentStatus.PENDING)
                .filter(a -> haversineKm(lat, lng, a.getPickupLatitude(), a.getPickupLongitude()) <= radius)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public DeliveryAssignmentResponseDto acceptAssignment(UUID deliveryPartnerUserId, UUID orderId) {
        requireApprovedPartner(deliveryPartnerUserId);

        DeliveryAssignment assignment = assignmentRepository
                .findByOrderIdAndStatusIn(orderId, List.of(DeliveryAssignmentStatus.PENDING))
                .orElseThrow(() -> new BusinessException("DELIVERY_ASSIGNMENT_NOT_AVAILABLE", "No pending assignment for this order", HttpStatus.NOT_FOUND));

        if (Instant.now().isAfter(assignment.getExpiresAt())) {
            throw new BusinessException("DELIVERY_ASSIGNMENT_EXPIRED", "This assignment has expired", HttpStatus.GONE);
        }

        assignment.setStatus(DeliveryAssignmentStatus.ACCEPTED);
        assignment.setDeliveryPartnerUserId(deliveryPartnerUserId);
        assignmentRepository.save(assignment);

        customerOrderService.acceptOrder(orderId);

        return mapToDto(assignment);
    }

    @Override
    public DeliveryAssignmentResponseDto rejectAssignment(UUID deliveryPartnerUserId, UUID orderId) {
        DeliveryAssignment assignment = assignmentRepository
                .findByOrderIdAndStatusIn(orderId, List.of(DeliveryAssignmentStatus.PENDING))
                .orElseThrow(() -> new BusinessException("DELIVERY_ASSIGNMENT_NOT_AVAILABLE", "No pending assignment for this order", HttpStatus.NOT_FOUND));

        assignment.setStatus(DeliveryAssignmentStatus.REJECTED);
        assignmentRepository.save(assignment);

        reassign(assignment, deliveryPartnerUserId);

        return mapToDto(assignment);
    }

    @Override
    public DeliveryAssignmentResponseDto markPickedUp(UUID deliveryPartnerUserId, UUID orderId) {
        DeliveryAssignment assignment = requireOwnedAssignment(deliveryPartnerUserId, orderId, DeliveryAssignmentStatus.ACCEPTED);

        assignment.setStatus(DeliveryAssignmentStatus.PICKED_UP);
        assignmentRepository.save(assignment);

        issueDeliveryOtp(assignment);
        customerOrderService.updateOrderStatus(orderId, "OUT_FOR_DELIVERY");

        return mapToDto(assignment);
    }

    @Override
    public void verifyDeliveryOtp(UUID deliveryPartnerUserId, UUID orderId, String otp) {
        DeliveryAssignment assignment = requireOwnedAssignment(deliveryPartnerUserId, orderId, DeliveryAssignmentStatus.PICKED_UP);

        DeliveryOtp deliveryOtp = otpRepository.findByAssignmentId(assignment.getId())
                .orElseThrow(() -> new BusinessException("DELIVERY_OTP_NOT_FOUND", "No OTP issued for this delivery", HttpStatus.NOT_FOUND));

        if (deliveryOtp.isVerified()) {
            throw new BusinessException("DELIVERY_OTP_ALREADY_VERIFIED", "OTP already verified");
        }
        if (Instant.now().isAfter(deliveryOtp.getExpiresAt())) {
            throw new BusinessException("DELIVERY_OTP_EXPIRED", "Delivery OTP has expired");
        }

        deliveryOtp.setAttempts(deliveryOtp.getAttempts() + 1);
        if (deliveryOtp.getAttempts() > MAX_OTP_ATTEMPTS) {
            otpRepository.save(deliveryOtp);
            throw new BusinessException("DELIVERY_OTP_MAX_ATTEMPTS", "Maximum OTP attempts exceeded");
        }

        if (!deliveryOtp.getOtpCode().equals(otp)) {
            otpRepository.save(deliveryOtp);
            throw new BusinessException("DELIVERY_OTP_INVALID", "Invalid OTP", HttpStatus.UNAUTHORIZED);
        }

        deliveryOtp.setVerified(true);
        otpRepository.save(deliveryOtp);
    }

    @Override
    public DeliveryAssignmentResponseDto completeDelivery(UUID deliveryPartnerUserId, UUID orderId) {
        DeliveryAssignment assignment = requireOwnedAssignment(deliveryPartnerUserId, orderId, DeliveryAssignmentStatus.PICKED_UP);

        DeliveryOtp deliveryOtp = otpRepository.findByAssignmentId(assignment.getId())
                .orElseThrow(() -> new BusinessException("DELIVERY_OTP_NOT_FOUND", "No OTP issued for this delivery", HttpStatus.NOT_FOUND));

        if (!deliveryOtp.isVerified()) {
            throw new BusinessException("DELIVERY_OTP_NOT_VERIFIED", "Delivery OTP must be verified before completing delivery", HttpStatus.BAD_REQUEST);
        }

        assignment.setStatus(DeliveryAssignmentStatus.DELIVERED);
        assignmentRepository.save(assignment);

        customerOrderService.updateOrderStatus(orderId, "DELIVERED");

        recordEarning(assignment);

        return mapToDto(assignment);
    }

    @Override
    public void expireStaleAssignments() {
        List<DeliveryAssignment> stale = assignmentRepository
                .findByStatusAndExpiresAtBefore(DeliveryAssignmentStatus.PENDING, Instant.now());

        for (DeliveryAssignment assignment : stale) {
            assignment.setStatus(DeliveryAssignmentStatus.EXPIRED);
            assignmentRepository.save(assignment);
            reassign(assignment, assignment.getDeliveryPartnerUserId());
        }
    }

    /** Sweeps every 15s to catch assignments whose 60-second acceptance window lapsed. */
    @Scheduled(fixedDelay = 15000)
    public void scheduledExpirySweep() {
        try {
            expireStaleAssignments();
        } catch (Exception e) {
            log.error("Error while expiring stale delivery assignments: {}", e.getMessage(), e);
        }
    }

    @Override
    public void createAssignmentForOrder(UUID orderId, double pickupLat, double pickupLng, double dropLat, double dropLng) {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrderId(orderId);
        assignment.setPickupLatitude(pickupLat);
        assignment.setPickupLongitude(pickupLng);
        assignment.setDropLatitude(dropLat);
        assignment.setDropLongitude(dropLng);
        assignment.setStatus(DeliveryAssignmentStatus.PENDING);
        assignment.setAssignedAt(Instant.now());
        assignment.setExpiresAt(Instant.now().plus(ASSIGNMENT_EXPIRY_SECONDS, ChronoUnit.SECONDS));
        assignmentRepository.save(assignment);

        if (findNearestAvailablePartner(pickupLat, pickupLng, List.of()) == null) {
            log.warn("No online, KYC-approved delivery partner currently available for order {}", orderId);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void reassign(DeliveryAssignment previous, UUID excludePartnerUserId) {
        List<UUID> alreadyTried = assignmentRepository.findByOrderId(previous.getOrderId()).stream()
                .map(DeliveryAssignment::getDeliveryPartnerUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (excludePartnerUserId != null) alreadyTried.add(excludePartnerUserId);

        DeliveryPartnerProfile nextPartner = findNearestAvailablePartner(
                previous.getPickupLatitude(), previous.getPickupLongitude(), alreadyTried);

        if (nextPartner == null) {
            log.warn("No further delivery partners available to reassign order {}", previous.getOrderId());
            return;
        }

        DeliveryAssignment newAssignment = new DeliveryAssignment();
        newAssignment.setOrderId(previous.getOrderId());
        newAssignment.setPickupLatitude(previous.getPickupLatitude());
        newAssignment.setPickupLongitude(previous.getPickupLongitude());
        newAssignment.setDropLatitude(previous.getDropLatitude());
        newAssignment.setDropLongitude(previous.getDropLongitude());
        newAssignment.setStatus(DeliveryAssignmentStatus.PENDING);
        newAssignment.setAssignedAt(Instant.now());
        newAssignment.setExpiresAt(Instant.now().plus(ASSIGNMENT_EXPIRY_SECONDS, ChronoUnit.SECONDS));
        assignmentRepository.save(newAssignment);
    }

    private DeliveryPartnerProfile findNearestAvailablePartner(double lat, double lng, List<UUID> excludeUserIds) {
        return partnerRepository.findByOnlineTrueAndKycStatus(DeliveryKycStatus.APPROVED).stream()
                .filter(p -> p.getCurrentLatitude() != null && p.getCurrentLongitude() != null)
                .filter(p -> !excludeUserIds.contains(p.getUserId()))
                .min((a, b) -> Double.compare(
                        haversineKm(lat, lng, a.getCurrentLatitude(), a.getCurrentLongitude()),
                        haversineKm(lat, lng, b.getCurrentLatitude(), b.getCurrentLongitude())))
                .orElse(null);
    }

    private void requireApprovedPartner(UUID userId) {
        DeliveryPartnerProfile partner = partnerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        if (partner.getKycStatus() != DeliveryKycStatus.APPROVED) {
            throw new BusinessException("DELIVERY_KYC_NOT_APPROVED", "KYC not approved", HttpStatus.FORBIDDEN);
        }
    }

    private DeliveryAssignment requireOwnedAssignment(UUID deliveryPartnerUserId, UUID orderId, DeliveryAssignmentStatus expectedStatus) {
        DeliveryAssignment assignment = assignmentRepository
                .findByOrderIdAndStatusIn(orderId, List.of(expectedStatus))
                .orElseThrow(() -> new BusinessException("DELIVERY_ASSIGNMENT_NOT_FOUND", "No assignment in expected state for this order", HttpStatus.NOT_FOUND));

        if (!deliveryPartnerUserId.equals(assignment.getDeliveryPartnerUserId())) {
            throw new BusinessException("DELIVERY_ASSIGNMENT_NOT_OWNED", "This assignment does not belong to you", HttpStatus.FORBIDDEN);
        }
        return assignment;
    }

    private void issueDeliveryOtp(DeliveryAssignment assignment) {
        String otpCode = String.format("%04d", new Random().nextInt(9999));
        DeliveryOtp otp = otpRepository.findByAssignmentId(assignment.getId()).orElseGet(DeliveryOtp::new);
        otp.setAssignmentId(assignment.getId());
        otp.setOtpCode(otpCode);
        otp.setExpiresAt(Instant.now().plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        otp.setVerified(false);
        otp.setAttempts(0);
        otpRepository.save(otp);

        // Mock: in production this goes to the customer via NotificationService.
        log.info("MOCK — Delivery completion OTP {} issued for assignment {}", otpCode, assignment.getId());
    }

    private void recordEarning(DeliveryAssignment assignment) {
        EarningRecord record = new EarningRecord();
        record.setDeliveryPartnerUserId(assignment.getDeliveryPartnerUserId());
        record.setOrderId(assignment.getOrderId());
        record.setAmount(FLAT_EARNING_PER_DELIVERY);
        earningRecordRepository.save(record);
    }

    private DeliveryAssignmentResponseDto mapToDto(DeliveryAssignment a) {
        return DeliveryAssignmentResponseDto.builder()
                .id(a.getId())
                .orderId(a.getOrderId())
                .status(a.getStatus())
                .pickupLatitude(a.getPickupLatitude())
                .pickupLongitude(a.getPickupLongitude())
                .dropLatitude(a.getDropLatitude())
                .dropLongitude(a.getDropLongitude())
                .assignedAt(a.getAssignedAt())
                .expiresAt(a.getExpiresAt())
                .build();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
