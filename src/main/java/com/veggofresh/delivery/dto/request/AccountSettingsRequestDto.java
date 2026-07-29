package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountSettingsRequestDto {
    private String fullName;

    @Email(message = "Must be a valid email address")
    private String email;

    private String vehicleType;
    private String vehicleColor;
    private Boolean pushNotificationsEnabled;
    private Boolean smsAlertsEnabled;
    private Boolean emailNewslettersEnabled;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
}
