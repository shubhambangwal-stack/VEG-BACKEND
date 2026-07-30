package com.veggofresh.customer.service.impl;

import com.veggofresh.admin.service.CouponService;
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
import java.util.List;
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
    private final CouponService couponService;

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

        Cart saved = cartRepository.save(cart);
        recomputePromo(saved);
        return mapToDto(cartRepository.save(saved));
    }

    @Override
    public CartResponseDto updateCartItem(UUID userId, UUID cartItemId, int quantity) {
        CartResponseDto cartDto = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cartDto.getId())
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Item not found in your cart", HttpStatus.NOT_FOUND));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        Cart cart = cartRepository.findById(cartDto.getId()).orElseThrow();
        recomputePromo(cart);
        return mapToDto(cartRepository.save(cart));
    }

    @Override
    public CartResponseDto removeCartItem(UUID userId, UUID cartItemId) {
        CartResponseDto cartDto = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cartDto.getId())
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Item not found in your cart", HttpStatus.NOT_FOUND));

        Cart cart = cartRepository.findById(cartDto.getId()).orElseThrow();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        Cart saved = cartRepository.save(cart);
        recomputePromo(saved);
        return mapToDto(cartRepository.save(saved));
    }

    @Override
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            cart.setPromoCode(null);
            cart.setPromoDiscount(null);
            cartRepository.save(cart);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartCount(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getCartRecommendations(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return productCatalogService.getDailyDeals();
        }

        // Gather unique recommendations based on categories of items in the cart
        List<ProductDto> recommendations = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            try {
                List<ProductDto> related = productCatalogService.getRelatedProducts(item.getProductId());
                if (related != null) {
                    for (ProductDto p : related) {
                        if (recommendations.stream().noneMatch(rec -> rec.getId().equals(p.getId()))) {
                            recommendations.add(p);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors for individual items
            }
        }
        return recommendations.stream().limit(6).collect(Collectors.toList());
    }

    @Override
    public CartResponseDto applyPromoCode(UUID userId, String code) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            ProductDto product = productCatalogService.getProductById(item.getProductId());
            if (product != null) {
                subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        BigDecimal discount = couponService.validateCoupon(code, subtotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_PROMO_CODE", "The promo code is invalid or does not meet criteria", HttpStatus.BAD_REQUEST);
        }

        cart.setPromoCode(code);
        cart.setPromoDiscount(discount);
        return mapToDto(cartRepository.save(cart));
    }

    @Override
    public CartResponseDto removePromoCode(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND));

        cart.setPromoCode(null);
        cart.setPromoDiscount(null);
        return mapToDto(cartRepository.save(cart));
    }

    private void recomputePromo(Cart cart) {
        if (cart.getPromoCode() != null) {
            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItem item : cart.getItems()) {
                ProductDto product = productCatalogService.getProductById(item.getProductId());
                if (product != null) {
                    subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
            BigDecimal discount = couponService.validateCoupon(cart.getPromoCode(), subtotal);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                cart.setPromoCode(null);
                cart.setPromoDiscount(null);
            } else {
                cart.setPromoDiscount(discount);
            }
        }
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
                        .productImageUrl(product.getImageUrl())
                        .build());
            }
        }

        BigDecimal deliveryFee = BigDecimal.valueOf(5.00); // flat delivery fee
        BigDecimal estimatedTax = total.multiply(BigDecimal.valueOf(0.05)); // 5% tax
        BigDecimal promoDiscount = cart.getPromoDiscount() != null ? cart.getPromoDiscount() : BigDecimal.ZERO;
        
        int itemCount = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();

        return CartResponseDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemsList)
                .totalAmount(total)
                .itemCount(itemCount)
                .deliveryFee(deliveryFee)
                .estimatedTax(estimatedTax)
                .promoCode(cart.getPromoCode())
                .promoDiscount(promoDiscount)
                .build();
    }
}
