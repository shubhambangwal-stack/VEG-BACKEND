package com.veggofresh.customer.controller;

import com.veggofresh.customer.dto.request.CartItemRequestDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.service.CartService;
import com.veggofresh.platform.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.veggofresh.platform.security.SecurityUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
public class CustomerCartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponseDto>> getCart() {
        CartResponseDto cart = cartService.getOrCreateCart(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartResponseDto>> addItem(
            @Valid @RequestBody CartItemRequestDto request) {
        CartResponseDto cart = cartService.addItemToCart(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart successfully"));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponseDto>> updateItemQuantity(
            @PathVariable UUID id,
            @RequestParam int quantity) {
        CartResponseDto cart = cartService.updateCartItem(SecurityUtils.getCurrentUserId(), id, quantity);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart item updated successfully"));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponseDto>> removeItem(
            @PathVariable UUID id) {
        CartResponseDto cart = cartService.removeCartItem(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart successfully"));
    }
}
