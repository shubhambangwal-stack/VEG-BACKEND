package com.veggofresh.admin.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_categories")
@Getter
@Setter
@NoArgsConstructor
public class CatalogCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Cloudinary public_id for {@link #imageUrl}, required to delete the old asset when
     * the category image is replaced. Internal only -- never exposed on CategoryResponseDto.
     */
    @Column(name = "image_public_id", length = 500)
    private String imagePublicId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
