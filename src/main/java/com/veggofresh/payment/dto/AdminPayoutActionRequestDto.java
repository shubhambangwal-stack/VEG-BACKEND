package com.veggofresh.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Body for POST /api/admin/payment/payouts/{id}/approve or /reject */
@Getter
@Setter
public class AdminPayoutActionRequestDto {

    @NotNull(message = "Action is required (APPROVE or REJECT)")
    private String action; // "APPROVE" or "REJECT"

    private String notes;
}
