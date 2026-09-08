package com.veggofresh.payment.repository;

import com.veggofresh.payment.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    /**
     * PESSIMISTIC_WRITE row lock -- required for correctness on credit/debit. This is a
     * financial ledger; a lost-update race (two concurrent credits both reading balance=100,
     * both writing balance=150 instead of 200) would silently corrupt real money, not just
     * data. Locking beats optimistic-retry here: the financial-correctness win is worth the
     * throughput cost, and it avoids needing retry logic on every write path.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") UUID userId);
}
