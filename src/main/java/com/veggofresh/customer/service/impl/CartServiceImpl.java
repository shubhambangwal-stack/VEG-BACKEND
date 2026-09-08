package com.veggofresh.customer.service.impl;

import com.veggofresh.admin.service.CouponService;
import com.veggofresh.customer.dto.request.CartItemRequestDto;
import com.veggofresh.customer.dto.response.CartItemResponseDto;
import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.entity.Address;
import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.customer.repository.AddressRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PHASE 2 — NEW ARCHITECTURE, multi-cart model (PROJECT_STATE section 2).
 *
 * VENDOR CATALOG PIVOT PATCH: ProductCatalogService methods now require a
 * latitude/longitude (radius eligibility depends on where the customer is).
 * Since add-to-cart/browsing happens before any checkout address is chosen,
 * this class resolves location from the customer's DEFAULT saved Address
 * (falling back to their first address if none is marked default) via
 * resolveLocation() below. If the customer has no address at all yet, cart
 * operations now require adding one first (ADDRESS_REQUIRED) -- a real
 * behavior change, flagged in NOTES_CUSTOMER.md, not silently introduced.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductCatalogService productCatalogService;
    private final CouponService couponService;

    @Override
    @Transactional(readOnly = true)
    public List<CartResponseDto> getOpenCarts(UUID userId) {
        List<Cart> carts = cartRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<CartResponseDto> result = new ArrayList<>();
        int index = 1;
        for (Cart cart : carts) {
            result.add(mapToDto(cart, index++));
        }
        return result;
    }

    @Override
    public List<CartResponseDto> addItemToCart(UUID userId, CartItemRequestDto request) {
        double[] location = resolveLocation(userId);
        ProductDto product = productCatalogService.getProductById(request.getProductId(), location[0], location[1]);
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "Product not found in catalog", HttpStatus.BAD_REQUEST);
        }

        Set<UUID> productVendorIds = productCatalogService.getShopIdsForProduct(request.getProductId(), location[0], location[1]);
        if (productVendorIds == null || productVendorIds.isEmpty()) {
            throw new BusinessException("PRODUCT_NOT_AVAILABLE", "This product currently has no vendor carrying it", HttpStatus.BAD_REQUEST);
        }

        List<Cart> openCarts = cartRepository.findByUserIdOrderByCreatedAtAsc(userId);

        Cart targetCart = null;
        for (Cart existing : openCarts) {
            Set<UUID> intersection = new HashSet<>(existing.getCandidateVendorIds());
            intersection.retainAll(productVendorIds);
            if (!intersection.isEmpty()) {
                existing.setCandidateVendorIds(intersection);
                targetCart = existing;
                break;
            }
        }

        if (targetCart == null) {
            targetCart = new Cart();
            targetCart.setUserId(userId);
            targetCart.setCandidateVendorIds(new HashSet<>(productVendorIds));
            targetCart = cartRepository.save(targetCart);
        }

        Optional<CartItem> existingItem = targetCart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(targetCart);
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            targetCart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        Cart saved = cartRepository.save(targetCart);
        recomputePromo(saved);
        cartRepository.save(saved);

        return getOpenCarts(userId);
    }

    @Override
    public List<CartResponseDto> updateCartItem(UUID userId, UUID cartItemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndCart_UserId(cartItemId, userId)
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Item not found in your cart", HttpStatus.NOT_FOUND));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        Cart cart = item.getCart();
        recomputePromo(cart);
        cartRepository.save(cart);

        return getOpenCarts(userId);
    }

    @Override
    public List<CartResponseDto> removeCartItem(UUID userId, UUID cartItemId) {
        CartItem item = cartItemRepository.findByIdAndCart_UserId(cartItemId, userId)
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND", "Item not found in your cart", HttpStatus.NOT_FOUND));

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        // Carts are static once formed — deliberately NOT recomputing
        // candidateVendorIds here even though removal can leave the cart
        // more fragmented than strictly necessary (confirmed simplification,
        // PROJECT_STATE section 2).
        recomputePromo(cart);
        cartRepository.save(cart);

        return getOpenCarts(userId);
    }

    @Override
    public void clearCart(UUID userId, UUID cartId) {
        cartRepository.findByIdAndUserId(cartId, userId).ifPresent(cart -> {
            cart.softDelete();
            cartRepository.save(cart);
        });
    }

    @Override
    public void clearAllCarts(UUID userId) {
        cartRepository.findByUserIdOrderByCreatedAtAsc(userId).forEach(cart -> {
            cart.softDelete();
            cartRepository.save(cart);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartCount(UUID userId) {
        return cartRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .flatMap(c -> c.getItems().stream())
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getCartRecommendations(UUID userId) {
        double[] location = resolveLocation(userId);
        List<Cart> carts = cartRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<CartItem> allItems = carts.stream()
                .flatMap(c -> c.getItems().stream())
                .collect(Collectors.toList());

        if (allItems.isEmpty()) {
            return productCatalogService.getDailyDeals(location[0], location[1]);
        }

        List<ProductDto> recommendations = new ArrayList<>();
        for (CartItem item : allItems) {
            try {
                List<ProductDto> related = productCatalogService.getRelatedProducts(item.getProductId(), location[0], location[1]);
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
    public List<CartResponseDto> applyPromoCode(UUID userId, UUID cartId, String code) {
        Cart cart = cartRepository.findByIdAndUserId(cartId, userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND));

        BigDecimal subtotal = computeSubtotal(cart);
        BigDecimal discount = couponService.validateCoupon(code, subtotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_PROMO_CODE", "The promo code is invalid or does not meet criteria", HttpStatus.BAD_REQUEST);
        }

        cart.setPromoCode(code);
        cart.setPromoDiscount(discount);
        cartRepository.save(cart);

        return getOpenCarts(userId);
    }

    @Override
    public List<CartResponseDto> removePromoCode(UUID userId, UUID cartId) {
        Cart cart = cartRepository.findByIdAndUserId(cartId, userId)
                .orElseThrow(() -> new BusinessException("CART_NOT_FOUND", "Cart not found", HttpStatus.NOT_FOUND));

        cart.setPromoCode(null);
        cart.setPromoDiscount(null);
        cartRepository.save(cart);

        return getOpenCarts(userId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    private BigDecimal computeSubtotal(Cart cart) {
        double[] location = resolveLocation(cart.getUserId());
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            ProductDto product = safeGetProduct(item.getProductId(), location);
            if (product != null) {
                subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        return subtotal;
    }

    private void recomputePromo(Cart cart) {
        if (cart.getPromoCode() != null) {
            BigDecimal subtotal = computeSubtotal(cart);
            BigDecimal discount = couponService.validateCoupon(cart.getPromoCode(), subtotal);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                cart.setPromoCode(null);
                cart.setPromoDiscount(null);
            } else {
                cart.setPromoDiscount(discount);
            }
        }
    }

    private CartResponseDto mapToDto(Cart cart, int index) {
        double[] location = resolveLocation(cart.getUserId());
        BigDecimal total = BigDecimal.ZERO;
        var itemsList = new ArrayList<CartItemResponseDto>();

        for (CartItem item : cart.getItems()) {
            ProductDto product = safeGetProduct(item.getProductId(), location);
            if (product != null) {
                BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(subTotal);

                itemsList.add(CartItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(product.getName())
                        .unitPrice(product.getPrice())
                        .unit(product.getUnit())
                        .quantity(item.getQuantity())
                        .subTotal(subTotal)
                        .productImageUrl(product.getImageUrl())
                        .build());
            }
        }

        BigDecimal deliveryFee = BigDecimal.valueOf(5.00);
        BigDecimal estimatedTax = total.multiply(BigDecimal.valueOf(0.05));
        BigDecimal promoDiscount = cart.getPromoDiscount() != null ? cart.getPromoDiscount() : BigDecimal.ZERO;

        int itemCount = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();

        return CartResponseDto.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .cartLabel("Cart " + index)
                .items(itemsList)
                .totalAmount(total)
                .itemCount(itemCount)
                .deliveryFee(deliveryFee)
                .estimatedTax(estimatedTax)
                .promoCode(cart.getPromoCode())
                .promoDiscount(promoDiscount)
                .build();
    }

    /**
     * VENDOR CATALOG PIVOT PATCH: resolves a reference location for radius
     * eligibility checks -- the customer's default saved Address, falling
     * back to their first address if none is marked default. Throws if they
     * have no address at all yet, since eligibility genuinely can't be
     * computed without one. This is a minimal compile-fix decision, not a
     * final design call -- see NOTES_CUSTOMER.md.
     */
    private double[] resolveLocation(UUID userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        Address reference = addresses.stream()
                .filter(Address::isDefault)
                .findFirst()
                .orElse(addresses.isEmpty() ? null : addresses.get(0));

        if (reference == null) {
            throw new BusinessException("ADDRESS_REQUIRED",
                    "Add a delivery address before browsing or adding items to your cart", HttpStatus.BAD_REQUEST);
        }
        return new double[]{reference.getLatitude(), reference.getLongitude()};
    }

    /**
     * Vendor's getProductById throws rather than returning null on
     * not-found/not-eligible. Wrapping it here restores the graceful
     * per-item skip the original `if (product != null)` checks throughout
     * this class visually intended -- now meaningfully reachable, since
     * radius eligibility makes "this one item became unavailable" a real,
     * expected case rather than a rare one.
     */
    private ProductDto safeGetProduct(UUID productId, double[] location) {
        try {
            return productCatalogService.getProductById(productId, location[0], location[1]);
        } catch (Exception e) {
            return null;
        }
    }
}
