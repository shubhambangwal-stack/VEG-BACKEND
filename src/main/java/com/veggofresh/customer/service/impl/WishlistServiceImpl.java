package com.veggofresh.customer.service.impl;

import com.veggofresh.customer.entity.Address;
import com.veggofresh.customer.entity.Wishlist;
import com.veggofresh.customer.repository.AddressRepository;
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

/**
 * VENDOR CATALOG PIVOT PATCH: ProductCatalogService methods now require a
 * latitude/longitude. Resolves location the same way CartServiceImpl does --
 * the customer's default saved Address. See NOTES_CUSTOMER.md.
 *
 * Also fixed here: getWishlist()'s `.filter(Objects::nonNull)` was already
 * dead code before this patch -- Vendor's getProductById has always thrown
 * rather than returned null on a missing/ineligible product, both before and
 * after the catalog pivot. One unavailable wishlist item would have crashed
 * the whole list. Now wrapped with safeGetProduct() so it's actually
 * reachable and does what it always visually intended to do.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final AddressRepository addressRepository;
    private final ProductCatalogService productCatalogService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getWishlist(UUID userId) {
        double[] location = resolveLocation(userId);
        List<Wishlist> items = wishlistRepository.findByUserId(userId);
        return items.stream()
                .map(item -> safeGetProduct(item.getProductId(), location))
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
        double[] location = resolveLocation(userId);
        List<ProductDto> wishlist = getWishlist(userId);
        if (wishlist.isEmpty()) {
            return productCatalogService.getDailyDeals(location[0], location[1]);
        }

        List<ProductDto> recommendations = new ArrayList<>();
        for (ProductDto p : wishlist) {
            try {
                List<ProductDto> related = productCatalogService.getRelatedProducts(p.getId(), location[0], location[1]);
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
        double[] location = resolveLocation(userId);
        ProductDto product = productCatalogService.getProductById(productId, location[0], location[1]);
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

    private double[] resolveLocation(UUID userId) {
        List<com.veggofresh.customer.entity.Address> addresses = addressRepository.findByUserId(userId);
        Address reference = addresses.stream()
                .filter(Address::isDefault)
                .findFirst()
                .orElse(addresses.isEmpty() ? null : addresses.get(0));

        if (reference == null) {
            throw new BusinessException("ADDRESS_REQUIRED",
                    "Add a delivery address before using your wishlist", HttpStatus.BAD_REQUEST);
        }
        return new double[]{reference.getLatitude(), reference.getLongitude()};
    }

    private ProductDto safeGetProduct(UUID productId, double[] location) {
        try {
            return productCatalogService.getProductById(productId, location[0], location[1]);
        } catch (Exception e) {
            return null;
        }
    }
}
