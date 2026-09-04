package com.veggofresh.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Body for POST /api/payment/wallet/withdraw */
@Getter
@Setter
public class WithdrawRequestDto {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum withdrawal amount is ₹1")
    private BigDecimal amount;
}
