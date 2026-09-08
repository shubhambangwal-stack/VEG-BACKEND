package com.veggofresh.admin.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Request for {@code PUT /api/admin/catalog/products/{id}/images/reorder}.
 *
 * <p>{@code imageIds} must contain every image ID currently belonging to the product,
 * in the new desired display order (position 0 becomes the new cover image). Partial
 * lists are rejected -- reordering is all-or-nothing so the sort order never ends up
 * partially applied.
 */
@Getter
@Setter
public class ProductImageReorderRequestDto {

    @NotEmpty(message = "imageIds is required and must include every image belonging to this product")
    private List<UUID> imageIds;
}
