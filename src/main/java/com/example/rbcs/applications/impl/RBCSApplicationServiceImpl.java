package com.example.rbcs.applications.impl;

import com.example.rbcs.applications.AccountApplicationService;
import com.example.rbcs.applications.TransactionApplicationService;
import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.entity.Transaction;
import com.example.rbcs.domain.event.TransactionExecuteRequest;
import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.repository.AccountRepository;
import com.example.rbcs.domain.repository.TransactionRepository;
import com.example.rbcs.domain.service.AccountService;
import com.example.rbcs.domain.service.TransactionService;
import com.example.rbcs.infrastructure.sqs.SqsPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RBCSApplicationServiceImpl implements AccountApplicationService, TransactionApplicationService {
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RedissonClient redissonClient;
    private final SqsPublisher sqsPublisher;
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
    public Transaction createTransferTransaction(Long fromAccountId, Long toAccountId, Long amount) {
        Account source = null;
        Account target = null;
        final var accounts = accountService.getValidAccounts(Stream.of(fromAccountId, toAccountId).filter(Objects::nonNull).toList());
        for (Account account : accounts) {
            if (Objects.equals(account.getId(), fromAccountId)) {
                source = account;
            } else if (Objects.equals(account.getId(), toAccountId)) {
                target = account;
            }
        }

        return transactionService.createTransaction(source, target, Transaction.Type.TRANSFER, amount);
    }

    @Override
    public Transaction createWithdrawTransaction(Long accountId, Long amount) {
        return transactionService.createTransaction(accountService.getValidAccount(accountId), null, Transaction.Type.WITHDRAWAL, amount);
    }

    @Override
    public Transaction createDepositTransaction(Long accountId, Long amount) {
        return transactionService.createTransaction(accountService.getValidAccount(accountId), null, Transaction.Type.DEPOSIT, amount);
    }

    @EventListener
    public void onTransactionCreated(TransactionExecuteRequest request) {
        transactionService.executeTransaction(request.transactionId());
    }

    /**
     * 每小时执行一次，获取一个小时前的1000条还处于pending的交易，重新执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void executePendingTransactions() {
        var lock = redissonClient.getLock("pending-transactions-re-execute");
        try {
            if (lock.tryLock(1, 600, TimeUnit.SECONDS)) {
                Date dateHourBefore = new Date(System.currentTimeMillis() - 1000 * 60 * 60);
                var transactions = transactionRepository.findAllByStatusAndCreatedAtLessThanEqual(Transaction.Status.PENDING, dateHourBefore, PageRequest.of(0, 1000));
                log.info("Executing {} pending transactions on scheduler", transactions.size());
                transactions.forEach(transaction -> sqsPublisher.publish(transaction.getId()));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
