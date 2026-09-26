package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.CategoryDto;
import com.veggofresh.vendor.dto.CategoryTreeDto;
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

    /**
     * NON-THROWING variant of {@link #getProductById}. Returns
     * {@link Optional#empty()} instead of throwing when the product is missing,
     * inactive, or has no vendor carrying it in range of the given location.
     *
     * <p>⚠️ Use THIS, not {@code getProductById}, when "not available near you"
     * is a normal per-item outcome rather than an error -- specifically when
     * resolving products belonging to rows that already exist (a cart's items, a
     * wishlist's entries, an order's lines).
     *
     * <p>Why this matters, in hard-won detail: {@code getProductById} throws a
     * {@code BusinessException}, and that exception is raised from inside a
     * nested {@code @Transactional} method. When such a method fails while
     * participating in a caller's transaction, Spring marks the SHARED
     * transaction rollback-only. Any caller that catches the exception and
     * carries on then appears to succeed, but fails much later at commit with:
     *
     * <pre>
     * UnexpectedRollbackException: Transaction silently rolled back
     * because it has been marked as rollback-only
     * </pre>
     *
     * <p>{@code noRollbackFor = BusinessException.class} on the throwing method
     * does NOT prevent this, because the rollback-only flag is set by the
     * innermost transactional advice the exception passes through, not by the
     * one that declares {@code noRollbackFor}. Since the nested read helper
     * ({@code AdminProductServiceImpl}) has no such attribute, the flag sticks.
     *
     * <p>So: the only reliable way to tolerate "this product is unavailable" is
     * to never raise an exception for it in the first place. This method does
     * exactly that -- a pure read that cannot mark anything rollback-only.
     */
    java.util.Optional<ProductDto> findEligibleProductById(UUID catalogProductId, double latitude, double longitude);

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

    /**
     * NEW -- one category with its subcategories and each subcategory's eligible
     * (radius-filtered, listed) products, all nested in one response. Categories/
     * subcategories themselves are not radius-filtered, matching browseCategories()/
     * browseSubcategories() above; only the nested products are.
     */
    CategoryTreeDto getCategoryTree(UUID categoryId, double latitude, double longitude);

    /**
     * NEW -- the entire active catalog, every category with every subcategory with
     * every eligible product nested inside, in one response. Intended for testing/
     * admin tooling, not the production mobile app -- cost scales with total catalog
     * size since radius-eligibility must be computed for every product across every
     * subcategory in a single call. See ProductCatalogServiceImpl for the same
     * overfetch-and-filter-in-Java caveat that already applies to searchProducts().
     */
    List<CategoryTreeDto> getFullCatalogTree(double latitude, double longitude);
}
