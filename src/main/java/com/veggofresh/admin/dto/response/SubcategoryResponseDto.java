package com.veggofresh.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryResponseDto {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String name;

    /** New capability -- subcategories had no image field at all before this. */
    private String imageUrl;

    private int displayOrder;
    private boolean isActive;
}
