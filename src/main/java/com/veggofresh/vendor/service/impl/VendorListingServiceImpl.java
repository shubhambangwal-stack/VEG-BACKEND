package com.veggofresh.vendor.service.impl;

import com.veggofresh.admin.dto.response.ProductResponseDto;
import com.veggofresh.admin.service.AdminProductService;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.response.VendorListingDto;
import com.veggofresh.vendor.entity.Shop;
import com.veggofresh.vendor.entity.VendorListing;
import com.veggofresh.vendor.repository.ShopRepository;
import com.veggofresh.vendor.repository.VendorListingRepository;
import com.veggofresh.vendor.service.VendorListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * NEW ARCHITECTURE — replaces VendorProductServiceImpl. Calls Admin's
 * AdminProductService (cross-module interface, never Admin's @Entity
 * directly) for catalog data, merges in this shop's own isListed state
 * from VendorListingRepository.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VendorListingServiceImpl implements VendorListingService {

    private final AdminProductService adminProductService;
    private final VendorListingRepository vendorListingRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VendorListingDto> browseCatalog(UUID ownerUserId, String search, UUID categoryId, UUID subcategoryId, Pageable pageable) {
        Shop shop = requireShop(ownerUserId);

        Page<ProductResponseDto> catalogPage = adminProductService.searchProducts(search, categoryId, subcategoryId, pageable);

        // Admin's search doesn't filter by isActive itself -- exclude deactivated
        // catalog products from what a vendor can browse/list here.
        List<VendorListingDto> content = catalogPage.getContent().stream()
                .filter(ProductResponseDto::isActive)
                .map(p -> toDto(p, shop.getId()))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, catalogPage.getTotalElements());
    }

    @Override
    public VendorListingDto setListed(UUID ownerUserId, UUID catalogProductId, boolean listed) {
        Shop shop = requireShop(ownerUserId);

        // Validates the catalog product actually exists in Admin's catalog --
        // throws CATALOG_PRODUCT_NOT_FOUND (from Admin's own service) otherwise.
        ProductResponseDto catalogProduct = adminProductService.getProductById(catalogProductId);
        if (!catalogProduct.isActive()) {
            throw new BusinessException("CATALOG_PRODUCT_INACTIVE", "This catalog product is not currently active", HttpStatus.BAD_REQUEST);
        }

        VendorListing listing = vendorListingRepository.findByShopIdAndCatalogProductId(shop.getId(), catalogProductId)
                .orElseGet(() -> {
                    VendorListing newListing = new VendorListing();
                    newListing.setShop(shop);
                    newListing.setCatalogProductId(catalogProductId);
                    return newListing;
                });

        listing.setListed(listed);
        VendorListing saved = vendorListingRepository.save(listing);

        return toDto(catalogProduct, shop.getId(), saved.isListed());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorListingDto> getMyListings(UUID ownerUserId, Boolean isListedFilter, Pageable pageable) {
        Shop shop = requireShop(ownerUserId);

        Page<VendorListing> listingsPage = vendorListingRepository.findByShopIdAndOptionalListedStatus(
                shop.getId(), isListedFilter, pageable);

        List<VendorListingDto> content = listingsPage.getContent().stream()
                .map(listing -> {
                    // Individual failures (e.g. Admin deactivated/removed the item since
                    // listing) shouldn't break the whole screen -- skip silently.
                    try {
                        ProductResponseDto p = adminProductService.getProductById(listing.getCatalogProductId());
                        return toDto(p, shop.getId(), listing.isListed());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, listingsPage.getTotalElements());
    }

    @Override
    public void deleteListing(UUID ownerUserId, UUID catalogProductId) {
        Shop shop = requireShop(ownerUserId);

        VendorListing listing = vendorListingRepository.findByShopIdAndCatalogProductId(shop.getId(), catalogProductId)
                .orElseThrow(() -> new BusinessException("VENDOR_LISTING_NOT_FOUND",
                        "You haven't added this product to your storefront", HttpStatus.NOT_FOUND));

        // Soft delete -- entity's @Where(deleted_at IS NULL) means this row becomes
        // invisible everywhere from this point on (mine, the isListed lookup used by
        // browseCatalog/setListed, etc). Works the same whether the item is currently
        // listed or unlisted; no separate unlist step required first.
        listing.softDelete();
        vendorListingRepository.save(listing);
    }

    private VendorListingDto toDto(ProductResponseDto p, UUID shopId) {
        boolean isListed = vendorListingRepository.findByShopIdAndCatalogProductId(shopId, p.getId())
                .map(VendorListing::isListed)
                .orElse(false);
        return toDto(p, shopId, isListed);
    }

    private VendorListingDto toDto(ProductResponseDto p, UUID shopId, boolean isListed) {
        return VendorListingDto.builder()
                .catalogProductId(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .categoryName(p.getCategoryName())
                .subcategoryName(p.getSubcategoryName())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .isListed(isListed)
                .build();
    }

    private Shop requireShop(UUID ownerUserId) {
        return shopRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> new BusinessException("VENDOR_SHOP_NOT_FOUND", "Shop not found", HttpStatus.NOT_FOUND));
    }
}
