package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.CartItemRequestDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.vendor.dto.ProductDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CartService {
    CartResponseDto getOrCreateCart(UUID userId);
    CartResponseDto addItemToCart(UUID userId, CartItemRequestDto request);
    CartResponseDto updateCartItem(UUID userId, UUID cartItemId, int quantity);
    CartResponseDto removeCartItem(UUID userId, UUID cartItemId);
    void clearCart(UUID userId);

    /** GAP 13 — lightweight cart item count for badge */
    int getCartCount(UUID userId);

    /** GAP 13 — "Pairs well with" complementary products */
    List<ProductDto> getCartRecommendations(UUID userId);

    /** GAP 13 — validate and apply promo code via Admin CouponService */
    CartResponseDto applyPromoCode(UUID userId, String code);

    /** GAP 13 — remove applied promo code */
    CartResponseDto removePromoCode(UUID userId);
}
