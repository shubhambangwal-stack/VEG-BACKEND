package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.dto.request.CartItemRequestDto;
import com.veggofresh.customer.dto.response.CartItemResponseDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.customer.repository.CartItemRepository;
import com.veggofresh.customer.repository.CartRepository;
import com.veggofresh.customer.service.CartService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductCatalogService productCatalogService;

    @Override
    public CartResponseDto getOrCreateCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });
        return mapToDto(cart);
    }

    @Override
    public CartResponseDto addItemToCart(UUID userId, CartItemRequestDto request) {
        CartResponseDto cartDto = getOrCreateCart(userId);
        Cart cart = cartRepository.findById(cartDto.getId())
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND));

        // Verify product exists in catalog
        ProductDto product = productCatalogService.getProductById(request.getProductId());
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "Product not found in catalog", HttpStatus.BAD_REQUEST);
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return mapToDto(cartRepository.save(cart));
    }

    @Override
    public CartResponseDto updateCartItem(UUID userId, UUID cartItemId, int quantity) {
        CartResponseDto cartDto = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cartDto.getId())
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Item not found in your cart", HttpStatus.NOT_FOUND));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        Cart cart = cartRepository.findById(cartDto.getId()).orElseThrow();
        return mapToDto(cart);
    }

    @Override
    public CartResponseDto removeCartItem(UUID userId, UUID cartItemId) {
        CartResponseDto cartDto = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cartDto.getId())
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Item not found in your cart", HttpStatus.NOT_FOUND));

        Cart cart = cartRepository.findById(cartDto.getId()).orElseThrow();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return mapToDto(cartRepository.save(cart));
    }

    @Override
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private CartResponseDto mapToDto(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        var itemsList = new ArrayList<CartItemResponseDto>();

        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            if (product != null) {
                BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(subTotal);

                itemsList.add(CartItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(product.getName())
                        .unitPrice(product.getPrice())
                        .quantity(item.getQuantity())
                        .subTotal(subTotal)
                        .build());
            }
        }

        return CartResponseDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemsList)
                .totalAmount(total)
                .build();
    }
}
