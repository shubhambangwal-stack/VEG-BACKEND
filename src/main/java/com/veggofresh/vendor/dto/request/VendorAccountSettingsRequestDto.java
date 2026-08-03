package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

/** Figma "Vendor Account Settings". */
@Getter
@Setter
public class VendorAccountSettingsRequestDto {
    private String fullName;

    @Email(message = "Must be a valid email address")
    private String email;

    private String businessPhone;
    private String businessLicenseNumber;
    private String profileImageUrl;
    private Boolean newOrderAlertsEnabled;
    private Boolean lowStockNotificationsEnabled;
    private Boolean payoutConfirmationsEnabled;
}
