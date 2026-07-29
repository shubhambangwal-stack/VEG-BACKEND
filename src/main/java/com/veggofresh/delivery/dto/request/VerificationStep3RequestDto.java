package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Figma "Bank Details, Step 3 of 3". FLAGGED: this data belongs to Payment
 *  module long-term -- stored in Delivery for now per team decision, see NOTES.md. */
@Getter
@Setter
public class VerificationStep3RequestDto {
    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "IFSC/routing code is required")
    private String ifscCode;

    @AssertTrue(message = "You must agree to the payout terms to finish onboarding")
    private boolean agreedToPayoutTerms;
}
