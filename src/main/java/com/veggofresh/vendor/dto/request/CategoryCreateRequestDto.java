package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequestDto {
    @NotBlank(message = "Category name is required")
    private String name;
    private String description;
}
