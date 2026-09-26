package com.veggofresh.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** NEW -- see CategoryTreeDto. products reuses ProductResponseDto directly
 *  (already carries imageUrls), no separate product-tree DTO needed. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryTreeDto {
    private UUID id;
    private String name;
    private String imageUrl;
    private List<ProductResponseDto> products;
}
