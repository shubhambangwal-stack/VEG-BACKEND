package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.UserBankAccountDto;

import java.util.List;
import java.util.UUID;

public interface BankAccountService {

    UserBankAccountDto saveOrUpdateBankAccount(UUID userId, UserBankAccountDto dto);

    UserBankAccountDto getBankAccountByUserId(UUID userId);

    /** Returns all bank accounts pending admin verification (isVerified = false). */
    List<UserBankAccountDto> getPendingBankAccounts();

    /**
     * Admin approves or rejects a bank account.
     * @param bankAccountId the ID of the UserBankAccount record
     * @param approve       true = verify; false = reject / mark unverified
     * @return updated DTO
     */
    UserBankAccountDto verifyBankAccount(UUID bankAccountId, boolean approve);
}
