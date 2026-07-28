package com.veggofresh.customer.service;

import com.veggofresh.vendor.dto.ProductDto;

import java.util.List;
import java.util.UUID;

public interface WishlistService {
    List<ProductDto> getWishlist(UUID userId);

    /** GAP 15 — filter wishlist by product category name */
    List<ProductDto> getWishlistByCategory(UUID userId, String category);

    /** GAP 15 — "You Might Also Like" based on categories in wishlist */
    List<ProductDto> getWishlistRecommendations(UUID userId);

    void addToWishlist(UUID userId, UUID productId);
    void removeFromWishlist(UUID userId, UUID productId);
}
