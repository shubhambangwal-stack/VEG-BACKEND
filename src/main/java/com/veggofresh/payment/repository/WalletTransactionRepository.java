package com.veggofresh.payment.repository;

import com.veggofresh.payment.entity.WalletTransaction;
import com.veggofresh.payment.entity.WalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    List<WalletTransaction> findByWalletIdAndTypeOrderByCreatedAtDesc(UUID walletId, WalletTransactionType type);
}