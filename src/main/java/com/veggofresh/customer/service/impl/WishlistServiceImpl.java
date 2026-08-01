package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.entity.Wishlist;
import com.veggofresh.customer.repository.WishlistRepository;
import com.veggofresh.customer.service.WishlistService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductCatalogService productCatalogService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getWishlist(UUID userId) {
        List<Wishlist> items = wishlistRepository.findByUserId(userId);
        return items.stream()
                .map(item -> productCatalogService.getProductById(item.getProductId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getWishlistByCategory(UUID userId, String category) {
        List<ProductDto> wishlist = getWishlist(userId);
        if (category == null || category.trim().isEmpty()) {
            return wishlist;
        }
        return wishlist.stream()
                .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category.trim()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getWishlistRecommendations(UUID userId) {
        List<ProductDto> wishlist = getWishlist(userId);
        if (wishlist.isEmpty()) {
            return productCatalogService.getDailyDeals();
        }

        // Return related products matching categories from wishlist items
        List<ProductDto> recommendations = new ArrayList<>();
        for (ProductDto p : wishlist) {
            try {
                List<ProductDto> related = productCatalogService.getRelatedProducts(p.getId());
                if (related != null) {
                    for (ProductDto r : related) {
                        if (recommendations.stream().noneMatch(rec -> rec.getId().equals(r.getId()))) {
                            recommendations.add(r);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors
            }
        }
        return recommendations.stream().limit(6).collect(Collectors.toList());
    }

    @Override
    public void addToWishlist(UUID userId, UUID productId) {
        // Verify product exists in catalog
        ProductDto product = productCatalogService.getProductById(productId);
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "Product not found in catalog", HttpStatus.BAD_REQUEST);
        }

        boolean alreadyInWishlist = wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent();
        if (!alreadyInWishlist) {
            Wishlist wishlist = new Wishlist();
            wishlist.setUserId(userId);
            wishlist.setProductId(productId);
            wishlistRepository.save(wishlist);
        }
    }

    @Override
    public void removeFromWishlist(UUID userId, UUID productId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new BusinessException("WISHLIST_ITEM_NOT_FOUND", "Item not found in your wishlist", HttpStatus.NOT_FOUND));

        wishlist.softDelete();
        wishlistRepository.save(wishlist);
    }
}
