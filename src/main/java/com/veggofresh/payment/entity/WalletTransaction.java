package com.veggofresh.payment.entity;

import com.veggofresh.payment.service.WalletTransactionReason;
import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Append-only ledger row. Never updated or deleted after creation -- balance history
 * must always be reconstructable from this table alone.
 */
@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
public class WalletTransaction extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WalletTransactionReason reason;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Wallet balance immediately after this transaction was applied -- makes the ledger self-auditing without recomputing a running sum. */
    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    /** e.g. the cancelled Order's id -- loose reference by id only, no cross-module entity relation. */
    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(length = 255)
    private String description;
}
