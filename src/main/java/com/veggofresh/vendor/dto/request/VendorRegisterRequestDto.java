package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorRegisterRequestDto {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Business type is required")
    private String businessType;
    
    @NotBlank(message = "Password is required")
    private String password;
}
