package com.veggofresh.payment.service;

import com.veggofresh.payment.dto.UserBankAccountDto;

import java.util.UUID;

public interface BankAccountService {

    UserBankAccountDto saveOrUpdateBankAccount(UUID userId, UserBankAccountDto dto);

    UserBankAccountDto getBankAccountByUserId(UUID userId);
}
