package com.veggofresh.vendor.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.UUID;

/**
 * NEW ARCHITECTURE — the catalog pivot (see NOTES_VENDOR.md).
 *
 * The bridge between a Shop and Admin's master catalog. A vendor no longer
 * creates their own products — they toggle {@link #isListed} against an
 * Admin-owned {@code CatalogProduct} (referenced by id only; Vendor must
 * NEVER import com.veggofresh.admin.entity.CatalogProduct directly — that's
 * a cross-module boundary violation, same class of bug as the old
 * OrderRepository.findByShopId issue in Customer).
 *
 * Deliberately NO price field (Admin's CatalogProduct.price is the only
 * price, everywhere) and NO stock field (vendor accepts/rejects each order
 * manually based on real stock at the time — see VendorOrderManagementService).
 */
@Entity
@Table(name = "vendor_listings")
@Getter
@Setter
@Where(clause = "deleted_at IS NULL")
public class VendorListing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    /** FK by id only into Admin's CatalogProduct — never a JPA relation. */
    @Column(name = "catalog_product_id", nullable = false)
    private UUID catalogProductId;

    @Column(name = "is_listed", nullable = false)
    private boolean isListed = false;
}
