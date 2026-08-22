package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.CategoryDto;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.dto.ShopDto;
import com.veggofresh.vendor.dto.SubcategoryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Interface contract provided by the Vendor module for the Customer module to
 * consume. Cross-module calls must ONLY go through this interface — never
 * import Vendor @Entity directly.
 *
 * ⚠️ BREAKING CHANGE — catalog pivot (see NOTES_VENDOR.md):
 * "product" now means an Admin-owned CatalogProduct id, resolved through
 * this shop's own VendorListing (isListed=true) — not Vendor's old,
 * now-deleted Product entity.
 *
 * ⚠️ Every method below except getAllCategories/browseCategories/
 * browseSubcategories now takes latitude/longitude. A product/shop is only
 * visible to a customer if at least one vendor with an active listing for it
 * is online, KYC-approved, AND within Admin's configured delivery radius of
 * that lat/long — "in range" is no longer optional context, it's required to
 * resolve almost anything here.
 *
 * ⚠️ CATEGORY FILTER BREAKING CHANGE (this round): searchProducts previously
 * took a category NAME string, resolved via a fragile case-insensitive match.
 * Now takes real categoryId/subcategoryId UUIDs, consistent with every other
 * filter in the system (Admin, Vendor). Customer's frontend gets those UUIDs
 * from browseCategories()/browseSubcategories() -- never types or guesses one.
 */
public interface ProductCatalogService {

    List<ShopDto> browseNearbyShops(double latitude, double longitude);

    /** Paginated, searchable, active-only categories -- powers the customer category picker. */
    Page<CategoryDto> browseCategories(String search, Pageable pageable);

    /** Paginated, searchable, active-only subcategories under one category. */
    Page<SubcategoryDto> browseSubcategories(UUID categoryId, String search, Pageable pageable);

    Page<ProductDto> searchProducts(String query, UUID categoryId, UUID subcategoryId, Double minPrice, Double maxPrice,
                                     double latitude, double longitude, Pageable pageable);

    ProductDto getProductById(UUID catalogProductId, double latitude, double longitude);

    List<ProductDto> getRelatedProducts(UUID catalogProductId, double latitude, double longitude);

    /**
     * NOW REAL (was always-empty before this round) -- returns eligible
     * products where Admin has set an originalPrice genuinely higher than
     * price (a real discount), radius-filtered the same as searchProducts.
     */
    List<ProductDto> getDailyDeals(double latitude, double longitude);

    /** Not radius-filtered — returns Admin's full active category list as a browsing aid. */
    List<CategoryDto> getAllCategories();

    /**
     * NEW — powers Customer's multi-cart vendor-overlap logic (PROJECT_STATE
     * section 2). Returns the shops that currently have this catalog product
     * listed AND are in range of the given location. Empty set = not
     * available to this customer right now, for any reason (not listed
     * anywhere, or listed only outside range, or listing vendor offline).
     */
    Set<UUID> getShopIdsForProduct(UUID catalogProductId, double latitude, double longitude);
}
