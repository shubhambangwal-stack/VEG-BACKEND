package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.request.CartItemRequestDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.vendor.dto.ProductDto;

import java.util.List;
import java.util.UUID;

/**
 * PHASE 2 — NEW ARCHITECTURE, multi-cart model (PROJECT_STATE section 2).
 *
 * A customer can have several concurrent OPEN carts. Adding an item checks
 * existing open carts in creation order for vendor overlap with the new
 * item: overlap found -> item joins that cart, narrowing its candidate-
 * vendor-set to the intersection; no overlap with any existing cart -> a
 * new cart is created with its own fresh candidate-vendor-set. Carts are
 * static once formed (no recompute/re-merge on item removal — confirmed
 * simplification).
 *
 * BREAKING CHANGE from Phase 1: every mutating method now returns the FULL
 * list of the user's open carts, not a single cart, since one call can
 * affect which cart an item lands in.
 */
public interface CartService {

    /** All of the user's currently open carts, oldest first ("Cart 1, Cart 2, ..."). */
    List<CartResponseDto> getOpenCarts(UUID userId);

    /** Adds an item; the system decides which existing cart it joins or creates a new one. */
    List<CartResponseDto> addItemToCart(UUID userId, CartItemRequestDto request);

    List<CartResponseDto> updateCartItem(UUID userId, UUID cartItemId, int quantity);

    List<CartResponseDto> removeCartItem(UUID userId, UUID cartItemId);

    /** Soft-deletes a single cart (called after that cart successfully converts to an order). */
    void clearCart(UUID userId, UUID cartId);

    /** Soft-deletes every open cart for the user. */
    void clearAllCarts(UUID userId);

    /** Total item count summed across ALL open carts, for the badge. */
    int getCartCount(UUID userId);

    /** "Pairs well with" recommendations aggregated across all open carts. */
    List<ProductDto> getCartRecommendations(UUID userId);

    /** Applies a promo code to one specific cart (promo is per-cart, not checkout-wide). */
    List<CartResponseDto> applyPromoCode(UUID userId, UUID cartId, String code);

    List<CartResponseDto> removePromoCode(UUID userId, UUID cartId);
}
