package com.veggofresh.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileSummaryDto {
    private UUID id;
    private String fullName;
    private String avatarUrl;
    private String email;
    private String phone;
    private int memberSinceYear;
    private long orderCount;
    private long favoritesCount;
    private long addressCount;
}
