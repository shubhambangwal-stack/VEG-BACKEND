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
