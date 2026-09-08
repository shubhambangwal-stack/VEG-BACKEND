package com.veggofresh.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request for {@code PUT /api/customer/onboarding/basic-info}.
 *
 * <p>Unlike {@link CustomerProfileUpdateRequestDto} (all fields optional, used for later
 * profile edits), {@code fullName} here is REQUIRED — this is the one-time, forced step
 * shown right after OTP verification, mirroring Delivery's and Vendor's basic-info step.
 * Email and avatar are deliberately not part of onboarding; both stay optional and are
 * only ever set afterward via the profile screen.
 */
@Getter
@Setter
public class CustomerBasicInfoRequestDto {

    @NotBlank(message = "Full name is required")
    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;
}
