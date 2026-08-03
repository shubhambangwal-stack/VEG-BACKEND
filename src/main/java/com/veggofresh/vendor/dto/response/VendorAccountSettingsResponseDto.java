package com.veggofresh.vendor.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VendorAccountSettingsResponseDto {
    private String fullName;
    private String email;
    private String businessPhone;
    private String businessLicenseNumber;
    private String profileImageUrl;
    private boolean newOrderAlertsEnabled;
    private boolean lowStockNotificationsEnabled;
    private boolean payoutConfirmationsEnabled;
}
