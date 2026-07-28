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

    /** Customer's display name (1–100 chars, no trailing spaces) */
    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    /**
     * Direct avatar URL.
     * Use this when the client uploads the image to cloud storage first and sends the resulting URL.
     * Max 2048 chars (standard URL length limit).
     */
    @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
    @Pattern(
        regexp = "^(https?://.*)?$",
        message = "Avatar URL must start with http:// or https://"
    )
    private String avatarUrl;
}
