package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Figma "Vendor Account Settings".
 *
 * <p>Bound via {@code @ModelAttribute} from {@code multipart/form-data} (not JSON) so the
 * owner's personal photo rides in the same call as the rest of the settings fields.
 * Every field remains optional with PATCH semantics -- only fields actually present in
 * the request are applied, exactly as before this change.
 */
@Getter
@Setter
public class VendorAccountSettingsRequestDto {
    private String fullName;

    @Email(message = "Must be a valid email address")
    private String email;

    private String businessPhone;
    private String businessLicenseNumber;

    /** Optional new personal photo (jpg/jpeg/png/webp). Omit to leave the current one unchanged. */
    private MultipartFile profileImage;

    private Boolean newOrderAlertsEnabled;
    private Boolean lowStockNotificationsEnabled;
    private Boolean payoutConfirmationsEnabled;
}
