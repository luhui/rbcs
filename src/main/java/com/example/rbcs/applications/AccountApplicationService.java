package com.example.rbcs.applications;

import com.example.rbcs.domain.entity.Account;

public interface AccountApplicationService {
    Account createAccount(String accountNumber);
    void freezeAccount(Long accountId);
    void deFreezeAccount(Long accountId);
    void activateAccount(Long accountId);
}
