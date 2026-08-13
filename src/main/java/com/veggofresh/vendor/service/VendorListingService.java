package com.veggofresh.vendor.service;

import com.veggofresh.vendor.dto.response.VendorListingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * NEW ARCHITECTURE — replaces the old VendorProductService (vendor no longer
 * creates products, only lists/unlists items from Admin's master catalog).
 */
public interface VendorListingService {

    /** Browse Admin's catalog with this vendor's current isListed state merged in. */
    Page<VendorListingDto> browseCatalog(UUID ownerUserId, String search, UUID categoryId, UUID subcategoryId, Pageable pageable);

    /** Toggle whether this vendor carries a given catalog product. */
    VendorListingDto setListed(UUID ownerUserId, UUID catalogProductId, boolean listed);

    /** Just the items this vendor currently has isListed = true. */
    List<VendorListingDto> getMyListings(UUID ownerUserId);
}
