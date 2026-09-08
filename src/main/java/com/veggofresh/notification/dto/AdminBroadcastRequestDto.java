package com.veggofresh.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin broadcast / announcement request. {@code recipientRole} is optional:
 * when null or blank, every user of every role receives the announcement;
 * otherwise only users whose role matches (CUSTOMER | VENDOR | DELIVERY | ADMIN).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcastRequestDto {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String body;

    private String recipientRole;

    /** Optional JSON context (e.g. a deep-link payload) forwarded verbatim. */
    private String data;
}