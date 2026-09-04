package com.veggofresh.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountSettingsResponseDto {
    private String fullName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String vehicleType;
    private String vehicleColor;
    private boolean pushNotificationsEnabled;
    private boolean smsAlertsEnabled;
    private boolean emailNewslettersEnabled;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;

    // ── Read-only: collected during onboarding KYC, not editable here ──────────
    // These were previously missing from this DTO entirely -- collected during
    // onboarding and then never shown anywhere again. Surfaced here as VIEW-ONLY;
    // changing a verified license/plate/vehicle-model/year after KYC approval
    // would undermine the verification, so there is deliberately no matching
    // field on AccountSettingsRequestDto.
    private String licenseNumber;
    private String plateNumber;
    private String vehicleModel;
    private Integer manufactureYear;
    private String cityOfOperation;
}
