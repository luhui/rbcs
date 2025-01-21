package com.example.rbcs.domain.service;

import com.example.rbcs.domain.entity.Account;

import java.util.List;

public interface AccountService {
    Account createAccount(String accountNo);
    
    void activateAccount(Long id);
    
    void freezeAccount(Long id);
    
    void defreezeAccount(Long id);

    void validateAccount(Long accountId);

    List<Account> getValidAccounts(List<Long> accountIds);

    Account getValidAccount(Long accountId);
}