package com.veggofresh.delivery.service.impl;

import com.veggofresh.delivery.dto.response.DeliveryProfileStatsResponseDto;
import com.veggofresh.delivery.entity.DeliveryAssignment;
import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import com.veggofresh.delivery.entity.DeliveryPartnerProfile;
import com.veggofresh.delivery.entity.DeliveryPartnerRating;
import com.veggofresh.delivery.entity.PartnerTier;
import com.veggofresh.delivery.repository.DeliveryAssignmentRepository;
import com.veggofresh.delivery.repository.DeliveryPartnerProfileRepository;
import com.veggofresh.delivery.repository.DeliveryPartnerRatingRepository;
import com.veggofresh.delivery.service.DeliveryStatsService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryStatsServiceImpl implements DeliveryStatsService {

    private static final List<DeliveryAssignmentStatus> COUNTED_OUTCOMES = List.of(
            DeliveryAssignmentStatus.DELIVERED, DeliveryAssignmentStatus.REJECTED, DeliveryAssignmentStatus.CANCELLED);

    private final DeliveryPartnerProfileRepository profileRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryPartnerRatingRepository ratingRepository;

    @Override
    public DeliveryProfileStatsResponseDto getStats(UUID deliveryPartnerUserId) {
        DeliveryPartnerProfile profile = profileRepository.findByUserId(deliveryPartnerUserId)
                .orElseThrow(() -> new BusinessException("DELIVERY_PROFILE_NOT_FOUND", "Delivery partner profile not found", HttpStatus.NOT_FOUND));

        // Only assignments this partner actually took ownership of and reached a
        // terminal outcome on -- excludes PENDING/EXPIRED rows never assigned to them.
        List<DeliveryAssignment> owned = assignmentRepository.findAll().stream()
                .filter(a -> deliveryPartnerUserId.equals(a.getDeliveryPartnerUserId()))
                .filter(a -> COUNTED_OUTCOMES.contains(a.getStatus()))
                .toList();

        long delivered = owned.stream().filter(a -> a.getStatus() == DeliveryAssignmentStatus.DELIVERED).count();
        double completionPercentage = owned.isEmpty() ? 0.0 : (delivered * 100.0) / owned.size();

        long daysActive = ChronoUnit.DAYS.between(profile.getCreatedAt(), Instant.now());

        List<DeliveryPartnerRating> ratings = ratingRepository.findByDeliveryPartnerUserId(deliveryPartnerUserId);
        Double averageRating = ratings.isEmpty() ? null
                : ratings.stream().mapToInt(DeliveryPartnerRating::getRatingValue).average().orElse(0.0);

        return DeliveryProfileStatsResponseDto.builder()
                .totalDeliveries(delivered)
                .completionPercentage(Math.round(completionPercentage * 10.0) / 10.0)
                .daysActive(daysActive)
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : null)
                .ratingCount(ratings.size())
                .tier(PartnerTier.fromDeliveryCount(delivered))
                .build();
    }
}
