package com.veggofresh.admin.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per product image. A {@link CatalogProduct} can have any number of these
 * (no fixed cap) -- at least 1 is required for a product to exist at all, enforced in
 * AdminProductServiceImpl, not here.
 *
 * <p>{@code sortOrder} supports full drag-to-reorder: position 0 is the cover image,
 * returned as {@code ProductResponseDto.imageUrl} for backward compatibility with every
 * existing downstream consumer (Vendor's ProductDto/VendorListingDto, Customer's browse
 * screens) that only ever expected a single thumbnail. The full ordered gallery is
 * returned separately as {@code ProductResponseDto.imageUrls}.
 */
@Entity
@Table(name = "catalog_product_images")
@Getter
@Setter
@NoArgsConstructor
public class CatalogProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private CatalogProduct product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /**
     * Cloudinary public_id, required to delete this asset when the image is removed
     * or the product is later cleaned up. Internal only -- never exposed on any DTO.
     */
    @Column(name = "public_id", length = 500)
    private String publicId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
