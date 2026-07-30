package com.veggofresh.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountSettingsResponseDto {
    private String fullName;
    private String phone;
    private String email;
    private String vehicleType;
    private String vehicleColor;
    private boolean pushNotificationsEnabled;
    private boolean smsAlertsEnabled;
    private boolean emailNewslettersEnabled;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
}
