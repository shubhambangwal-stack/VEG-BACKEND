package com.veggofresh.vendor.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Figma "Bank Details" — onboarding Step 3. Mirrors Delivery's VerificationStep3RequestDto. */
@Getter
@Setter
public class VendorBankDetailsRequestDto {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "IFSC/routing code is required")
    private String ifscCode;

    @AssertTrue(message = "You must agree to the payout terms to continue")
    private boolean agreedToPayoutTerms;
}
