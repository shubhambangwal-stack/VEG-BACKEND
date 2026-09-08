package com.veggofresh.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A row in the vendor's "browse the master catalog, toggle what I carry" screen.
 * Price/name/description/image come from Admin's CatalogProduct — a vendor
 * cannot edit any of it here, only isListed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorListingDto {
    private UUID catalogProductId;
    private String name;
    private String description;
    private String categoryName;
    private String subcategoryName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String unit;
    private Integer discountPercent;
    /** Cover image -- kept for backward compatibility with anything only expecting one thumbnail. */
    private String imageUrl;

    /**
     * GAP FIX: full ordered gallery -- was missing entirely before this round.
     * Populated straight from Admin's ProductResponseDto.imageUrls.
     */
    private List<String> imageUrls;

    private boolean isListed;
}
