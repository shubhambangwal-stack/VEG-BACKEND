package com.veggofresh.payment.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable ledger entry for a wallet.
 * Records are only ever appended � never updated or deleted.
 */
@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "deleted_at IS NULL")
public class WalletTransaction extends BaseEntity {

    @Column(name = "wallet_id", nullable = false, length = 36)
    private UUID walletId;

    /** The VegGoFresh order that caused this ledger entry. Null for withdrawals. */
    @Column(name = "order_id", length = 36)
    private UUID orderId;

    /** Razorpay payment ID, set on capture/refund events. Null otherwise. */
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String description;
}
