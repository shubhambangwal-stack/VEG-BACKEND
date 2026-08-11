package com.veggofresh.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestDto {

    @NotBlank(message = "Category name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;

    private String imageUrl;

    private int displayOrder = 0;
}