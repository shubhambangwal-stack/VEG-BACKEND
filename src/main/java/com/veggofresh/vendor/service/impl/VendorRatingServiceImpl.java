package com.veggofresh.vendor.service.impl;

import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.entity.VendorShopRating;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.repository.VendorShopRatingRepository;
import com.veggofresh.vendor.service.VendorRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorRatingServiceImpl implements VendorRatingService {

    private final VendorShopRatingRepository ratingRepository;
    private final ShopRepository shopRepository;

    @Override
    public void rateShop(UUID orderId, UUID shopId, UUID customerUserId, int ratingValue, String comment) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new BusinessException("VENDOR_RATING_INVALID", "Rating must be between 1 and 5", HttpStatus.BAD_REQUEST);
        }

        shopRepository.findByIdAndDeletedAtIsNull(shopId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));

        ratingRepository.findByOrderIdAndShopId(orderId, shopId).ifPresent(r -> {
            throw new BusinessException("VENDOR_RATING_ALREADY_SUBMITTED", "This shop has already been rated for this order", HttpStatus.BAD_REQUEST);
        });

        VendorShopRating rating = new VendorShopRating();
        rating.setOrderId(orderId);
        rating.setShopId(shopId);
        rating.setCustomerUserId(customerUserId);
        rating.setRatingValue(ratingValue);
        rating.setComment(comment);
        ratingRepository.save(rating);
    }
}
