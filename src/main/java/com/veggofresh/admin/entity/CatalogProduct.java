package com.veggofresh.admin.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Admin-owned master catalog product. NOT the same as Vendor's pre-pivot
 * com.veggofresh.vendor.entity.Product (legacy, vendor-owned) — see
 * NOTES_ADMIN.md, "Class-name collision with Vendor module."
 */
@Entity
@Table(name = "catalog_products")
@Getter
@Setter
@NoArgsConstructor
public class CatalogProduct extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CatalogCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private CatalogSubcategory subcategory;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
