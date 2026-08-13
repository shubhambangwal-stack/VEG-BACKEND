package com.veggofresh.customer.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating a customer profile.
 * All fields are optional — only non-null fields will be applied (PATCH semantics).
 */
@Getter
@Setter
public class CustomerProfileUpdateRequestDto {

    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
    @Pattern(
        regexp = "^(https?://.*)?$",
        message = "Avatar URL must start with http:// or https://"
    )
    private String avatarUrl;
}
