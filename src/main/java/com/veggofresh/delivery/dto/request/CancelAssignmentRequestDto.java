package com.veggofresh.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelAssignmentRequestDto {
    @NotBlank(message = "A cancellation reason is required")
    private String reason;
}
