package com.veggofresh.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBankAccountDto {

    private UUID id;
    private UUID userId;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String upiId;
    private boolean isVerified;
    private Instant createdAt;
    private Instant updatedAt;
}
