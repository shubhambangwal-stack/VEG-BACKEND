package com.veggofresh.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayoutRequestCreateDto {

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "1.00", message = "Minimum withdrawal amount is 1.00")
    private BigDecimal amount;
}
