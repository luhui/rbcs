package com.example.rbcs.domain.service;

import com.example.rbcs.domain.entity.Account;

import java.util.List;

public interface AccountService {
    Account createAccount(String accountNo);
    
    void activateAccount(Long id);
    
    void freezeAccount(Long id);
    
    void unfreezeAccount(Long id);

    void validateAccount(String accountNumber);

    List<Account> getValidAccounts(List<String> accountNumber);

    Account getValidAccount(String accountNumber);
}