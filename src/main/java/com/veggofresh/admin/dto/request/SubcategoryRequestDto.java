package com.veggofresh.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SubcategoryRequestDto {

    @NotNull(message = "categoryId is required")
    private UUID categoryId;

    @NotBlank(message = "Subcategory name is required")
    @Size(max = 150)
    private String name;

    private int displayOrder = 0;
}