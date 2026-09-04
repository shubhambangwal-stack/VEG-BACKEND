package com.veggofresh.admin.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_subcategories")
@Getter
@Setter
@NoArgsConstructor
public class CatalogSubcategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CatalogCategory category;

    @Column(nullable = false, length = 150)
    private String name;

    /** New capability -- subcategories had no image field at all before this. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Cloudinary public_id for {@link #imageUrl}, required to delete the old asset when
     * the subcategory image is replaced. Internal only -- never exposed on SubcategoryResponseDto.
     */
    @Column(name = "image_public_id", length = 500)
    private String imagePublicId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
