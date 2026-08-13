package com.veggofresh.admin.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductRequestDto {

    @NotBlank(message = "Product name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "categoryId is required")
    private UUID categoryId;

    @NotNull(message = "subcategoryId is required")
    private UUID subcategoryId;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    private String imageUrl;

    @NotBlank(message = "quantityUnit is required")
    @Size(max = 50)
    private String quantityUnit;

    @NotNull(message = "quantityValue is required")
    @DecimalMin(value = "0.01", message = "quantityValue must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal quantityValue;
}