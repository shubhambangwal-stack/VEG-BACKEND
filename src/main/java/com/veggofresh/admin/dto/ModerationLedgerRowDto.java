package com.veggofresh.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Row in the "User Moderation Ledger" table (screens 3 & 4 of admin images).
 * Columns: Customer Details, Account ID, Flags Raised, Primary Infraction,
 *          Account State, Moderation Actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationLedgerRowDto {

    /** e.g. "CUST-84920" */
    private String customerId;

    private UUID userId;

    /** Full name of the customer */
    private String fullName;

    /** Email shown under name */
    private String email;

    /**
     * Number of flags raised against this account.
     * UI shows: "0 Flags", "3 Flags", "8 Flags" etc.
     */
    private int flagsRaised;

    /**
     * Primary infraction description.
     * E.g., "Multiple payment failures", "Repeated cancellation abuse",
     * "Suspicious location change", "Promo code manipulation", "None"
     */
    private String primaryInfraction;

    /**
     * Account state label.
     * Values: "Active", "Blocked"
     */
    private String accountState;

    private boolean isBlocked;
}
