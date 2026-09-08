package com.veggofresh.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One product image, WITH its id -- unlike {@code ProductResponseDto.imageUrls} (plain
 * URL strings, for simple display consumers), this is returned by the image-management
 * endpoints (add/delete/reorder) since the admin UI needs the id to delete or reorder
 * a specific image.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponseDto {
    private UUID id;
    private String imageUrl;
    private int sortOrder;
}
