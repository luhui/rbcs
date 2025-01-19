package com.example.rbcs.domain.service;

import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.exception.AccountStatusInvalid;
import com.example.rbcs.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public void validateAccount(String accountNumber) {
        final var account = getAccount(accountNumber);
        account.assertActivateStatus();
    }

    @Override
    public List<Account> getValidAccounts(List<String> accountNumber) {
        final var accounts = accountRepository.findByAccountNumberIn(accountNumber);
        if (accounts.size() != accountNumber.size()) {
            throw new AccountNotFoundException();
        }
        for (Account account : accounts) {
            account.assertActivateStatus();
        }
        return accounts;
    }

    @Override
    public Account getValidAccount(String accountNumber) {
        final var account = getAccount(accountNumber);
        account.assertActivateStatus();
        return account;
    }

    @Override
    public Account createAccount(String accountNo) {
        return accountRepository.save(Account.builder().accountNumber(accountNo).build());
    }

    @Override
    public void activateAccount(Long id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        account.activate();
        accountRepository.save(account);
    }

    @Override
    public void freezeAccount(Long id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        account.freeze();
        accountRepository.save(account);
    }

    @Override
    public void unfreezeAccount(Long id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        account.deFreeze();
        accountRepository.save(account);
    }

    private Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow(AccountNotFoundException::new);
    }
}