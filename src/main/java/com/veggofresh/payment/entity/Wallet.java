package com.veggofresh.payment.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Unified wallet for all user types (CUSTOMER, VENDOR, DELIVERY, ADMIN).
 *
 * <p>One wallet per user. Auto-created on first access.
 *
 * <p>Balance is split into two buckets:
 * <ul>
 *   <li>{@code availableBalance} � spendable / withdrawable right now</li>
 *   <li>{@code reservedBalance}  � soft-held during an active checkout (acts like Razorpay's hold,
 *       but for wallet funds). Released back to available on timeout; permanently debited on vendor accept.</li>
 * </ul>
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "deleted_at IS NULL")
public class Wallet extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private UUID userId;

    /** Role of the wallet owner. Used for filtering in admin reports. */
    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "available_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "reserved_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal reservedBalance = BigDecimal.ZERO;
}
