package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bound via {@code @ModelAttribute} from {@code multipart/form-data} (not JSON) so the
 * avatar rides in the same call as the rest of account settings. Every field remains
 * optional with PATCH semantics -- only fields actually present in the request are
 * applied, exactly as before this change. Sending just {@code avatar} updates only the
 * photo, exactly like sending just {@code fullName} updates only the name.
 *
 * <p>Note: {@code licenseNumber}, {@code plateNumber}, {@code vehicleModel},
 * {@code manufactureYear}, and {@code cityOfOperation} are intentionally NOT here --
 * those are KYC-verified fields collected during onboarding and are read-only in
 * {@link com.veggofresh.delivery.dto.response.AccountSettingsResponseDto}, editable only
 * through a future re-verification flow, not this endpoint.
 */
@Getter
@Setter
public class AccountSettingsRequestDto {
    private String fullName;

    @Email(message = "Must be a valid email address")
    private String email;

    private String vehicleType;
    private String vehicleColor;

    /** Optional new personal photo (jpg/jpeg/png/webp). Omit to leave the current one unchanged. */
    private MultipartFile avatar;

    private Boolean pushNotificationsEnabled;
    private Boolean smsAlertsEnabled;
    private Boolean emailNewslettersEnabled;
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
}
