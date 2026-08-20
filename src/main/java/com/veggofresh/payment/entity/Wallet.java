package com.veggofresh.payment.entity;

import com.veggofresh.platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One wallet per user, regardless of role (Customer, Vendor, or Delivery partner).
 * No separate "userType" discriminator needed -- Auth's User.id (UUID) is already
 * globally unique across all roles, so userId alone is sufficient to key this table.
 * Role, if ever needed for display, is resolved via UserLookupService like everywhere
 * else in this codebase -- never duplicated here.
 *
 * NARROW SCOPE (this round): only credit/debit + balance + transaction history are
 * built. NOT built: soft-reservation (hold/release) semantics needed for the full
 * Razorpay-parity checkout wallet design in PROJECT_STATE section 6 -- that requires
 * real Payment/Razorpay integration to exist first, and is explicitly deferred. This
 * table and service are the real, correct foundation that design will build on top of,
 * not a placeholder that gets thrown away.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
public class Wallet extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
}
