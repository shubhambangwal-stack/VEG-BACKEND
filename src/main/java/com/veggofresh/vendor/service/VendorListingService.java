package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.response.VendorListingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * NEW ARCHITECTURE — replaces the old VendorProductService (vendor no longer
 * creates products, only lists/unlists items from Admin's master catalog).
 */
public interface VendorListingService {

    /** Browse Admin's catalog with this vendor's current isListed state merged in. */
    Page<VendorListingDto> browseCatalog(UUID ownerUserId, String search, UUID categoryId, UUID subcategoryId, Pageable pageable);

    /** Toggle whether this vendor carries a given catalog product. Does not remove it from "mine". */
    VendorListingDto setListed(UUID ownerUserId, UUID catalogProductId, boolean listed);

    /**
     * Paginated /mine. isListedFilter: null = ALL (listed + unlisted),
     * true = LISTED only, false = UNLISTED only. Unlisted items still appear
     * under ALL/UNLISTED -- only deleteListing() removes an item from mine.
     */
    Page<VendorListingDto> getMyListings(UUID ownerUserId, Boolean isListedFilter, Pageable pageable);

    /**
     * Permanently removes this catalog product from the vendor's storefront
     * (soft delete). Works regardless of current isListed value -- deleting a
     * currently-listed item auto-unlists it as part of the same call. Adding
     * the same catalogProductId again afterwards creates a brand-new row.
     */
    void deleteListing(UUID ownerUserId, UUID catalogProductId);
}
