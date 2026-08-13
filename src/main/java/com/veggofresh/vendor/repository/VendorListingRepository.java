package com.veggofresh.vendor.repository;

import com.veggofresh.vendor.entity.VendorListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorListingRepository extends JpaRepository<VendorListing, UUID> {

    Optional<VendorListing> findByShopIdAndCatalogProductId(UUID shopId, UUID catalogProductId);

    List<VendorListing> findByShopId(UUID shopId);

    long countByShopIdAndIsListedTrue(UUID shopId);

    /** All shops (via their listing rows) currently listing this catalog product. */
    List<VendorListing> findByCatalogProductIdAndIsListedTrue(UUID catalogProductId);
}
