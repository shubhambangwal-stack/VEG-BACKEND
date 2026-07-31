package com.veggofresh.delivery.service.impl;

import com.veggofresh.delivery.entity.DeliveryAssignment;
import com.veggofresh.delivery.entity.DeliveryAssignmentStatus;
import com.veggofresh.delivery.entity.DeliveryPartnerRating;
import com.veggofresh.delivery.repository.DeliveryAssignmentRepository;
import com.veggofresh.delivery.repository.DeliveryPartnerRatingRepository;
import com.veggofresh.delivery.service.DeliveryRatingService;
import com.veggofresh.platform.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryRatingServiceImpl implements DeliveryRatingService {

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryPartnerRatingRepository ratingRepository;

    @Override
    public void rateDeliveryPartner(UUID orderId, UUID customerUserId, int ratingValue, String comment) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new BusinessException("DELIVERY_RATING_INVALID", "Rating must be between 1 and 5", HttpStatus.BAD_REQUEST);
        }

        DeliveryAssignment assignment = assignmentRepository
                .findByOrderIdAndStatusIn(orderId, List.of(DeliveryAssignmentStatus.DELIVERED))
                .orElseThrow(() -> new BusinessException("DELIVERY_ASSIGNMENT_NOT_DELIVERED", "This order has no delivered assignment to rate", HttpStatus.BAD_REQUEST));

        ratingRepository.findByAssignmentId(assignment.getId()).ifPresent(r -> {
            throw new BusinessException("DELIVERY_RATING_ALREADY_SUBMITTED", "This delivery has already been rated", HttpStatus.BAD_REQUEST);
        });

        DeliveryPartnerRating rating = new DeliveryPartnerRating();
        rating.setAssignmentId(assignment.getId());
        rating.setDeliveryPartnerUserId(assignment.getDeliveryPartnerUserId());
        rating.setCustomerUserId(customerUserId);
        rating.setRatingValue(ratingValue);
        rating.setComment(comment);
        ratingRepository.save(rating);
    }
}
