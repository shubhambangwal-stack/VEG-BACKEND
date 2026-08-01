package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Figma "Vendor Onboarding Step 1 of 3: Basic Info". */
@Getter
@Setter
public class VendorBasicInfoRequestDto {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String businessPhone;

    @NotBlank(message = "Business type is required")
    private String businessType;
}
