package com.veggofresh.customer.service;

import java.util.List;
import java.util.UUID;

import com.veggofresh.vendor.dto.ProductDto;

public interface WishlistService {
    List<ProductDto> getWishlist(UUID userId);
    List<ProductDto> getWishlistByCategory(UUID userId, String category);
    List<ProductDto> getWishlistRecommendations(UUID userId);
    void addToWishlist(UUID userId, UUID productId);
    void removeFromWishlist(UUID userId, UUID productId);
}
