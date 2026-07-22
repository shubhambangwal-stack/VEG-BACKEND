package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.CartItemRequestDto;
import com.veggofresh.customer.dto.response.CartResponseDto;

import java.util.UUID;

public interface CartService {
    CartResponseDto getOrCreateCart(UUID userId);
    CartResponseDto addItemToCart(UUID userId, CartItemRequestDto request);
    CartResponseDto updateCartItem(UUID userId, UUID cartItemId, int quantity);
    CartResponseDto removeCartItem(UUID userId, UUID cartItemId);
    void clearCart(UUID userId);
}
