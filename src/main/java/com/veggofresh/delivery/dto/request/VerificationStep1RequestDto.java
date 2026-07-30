package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Figma "Personal Verification, Step 1 of 3". The license PHOTO is uploaded
 *  separately via POST /api/delivery/documents/LICENSE -- this only saves the
 *  structured license number and advances verificationStep once that photo exists. */
@Getter
@Setter
public class VerificationStep1RequestDto {
    @NotBlank(message = "Driver's license number is required")
    private String licenseNumber;
}
