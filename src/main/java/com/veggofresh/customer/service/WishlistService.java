package com.veggofresh.customer.service;

import com.veggofresh.vendor.dto.ProductDto;

import java.util.List;
import java.util.UUID;

public interface WishlistService {
    List<ProductDto> getWishlist(UUID userId);
    void addToWishlist(UUID userId, UUID productId);
    void removeFromWishlist(UUID userId, UUID productId);
}
