package com.veggofresh.payment.repository;

import com.veggofresh.payment.entity.UserBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBankAccountRepository extends JpaRepository<UserBankAccount, UUID> {

    Optional<UserBankAccount> findByUserId(UUID userId);
}
