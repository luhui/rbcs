package com.example.rbcs.applications.impl;

import com.example.rbcs.applications.AccountApplicationService;
import com.example.rbcs.applications.TransactionApplicationService;
import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.entity.Transaction;
import com.example.rbcs.domain.event.TransactionCreatedEvent;
import com.example.rbcs.domain.event.TransactionExecuteRequest;
import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.repository.AccountRepository;
import com.example.rbcs.domain.service.AccountService;
import com.example.rbcs.domain.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RBCSApplicationServiceImpl implements AccountApplicationService, TransactionApplicationService {
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    @Override
    public Account createAccount(String accountNumber) {
        final var acc = Account.builder()
                .accountNumber(accountNumber)
                .build();
        return accountRepository.save(acc);
    }

    @Override
    public void freezeAccount(Long accountId) {
        final var acc = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        acc.freeze();
        accountRepository.save(acc);
    }

    @Override
    public void deFreezeAccount(Long accountId) {
        final var acc = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        acc.deFreeze();
        accountRepository.save(acc);
    }

    @Override
    public void activateAccount(Long accountId) {
        final var acc = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
        acc.activate();
        accountRepository.save(acc);
    }

    @Override
    public Transaction createTransferTransaction(String fromAccountNo, String toAccountNo, Long amount) {
        Account source = null;
        Account target = null;
        final var accounts = accountService.getValidAccounts(Stream.of(fromAccountNo, toAccountNo).filter(Objects::nonNull).toList());
        for (Account account : accounts) {
            if (Objects.equals(account.getAccountNumber(), fromAccountNo)) {
                source = account;
            } else if (Objects.equals(account.getAccountNumber(), toAccountNo)) {
                target = account;
            }
        }

        return transactionService.createTransaction(source, target, Transaction.Type.TRANSFER, amount);
    }

    @Override
    public Transaction createWithdrawTransaction(String accountNo, Long amount) {
        return transactionService.createTransaction(accountService.getValidAccount(accountNo), null, Transaction.Type.WITHDRAWAL, amount);
    }

    @Override
    public Transaction createDepositTransaction(String accountNo, Long amount) {
        return transactionService.createTransaction(accountService.getValidAccount(accountNo), null, Transaction.Type.DEPOSIT, amount);
    }

    @EventListener
    public void onTransactionCreated(TransactionExecuteRequest request) {
        transactionService.executeTransaction(request.transactionId());
    }
}
