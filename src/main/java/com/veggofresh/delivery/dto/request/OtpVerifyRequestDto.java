package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpVerifyRequestDto {
    @NotBlank(message = "OTP is required")
    private String otp;
}
