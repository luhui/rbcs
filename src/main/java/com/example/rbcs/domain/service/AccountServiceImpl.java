package com.example.rbcs.domain.service;

import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.exception.AccountStatusInvalid;
import com.example.rbcs.domain.exception.DomainException;
import com.example.rbcs.domain.repository.AccountRepository;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final RedissonClient redissonClient;
    @Resource
    @Lazy
    AccountServiceImpl self;

    @Override
    public void validateAccount(Long accountId) {
        final var account = getAccount(accountId);
        account.assertActivateStatus();
    }

    @Override
    public List<Account> getValidAccounts(List<Long> accountIds) {
        final var accounts = accountRepository.findAllById(accountIds);
        if (accounts.size() != accountIds.size()) {
            throw new AccountNotFoundException();
        }
        for (Account account : accounts) {
            account.assertActivateStatus();
        }
        return accounts;
    }

    @Override
    public Account getValidAccount(Long accountId) {
        final var account = getAccount(accountId);
        account.assertActivateStatus();
        return account;
    }

    @Override
    public Account createAccount(String accountNo) {
        return accountRepository.save(Account.builder().accountNumber(accountNo).build());
    }

    @Override
    public void activateAccount(Long id) {
        self.requireAccountLock(id, () -> {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
            account.activate();
            accountRepository.save(account);
        });
    }

    @Override
    public void freezeAccount(Long id) {
        self.requireAccountLock(id, () -> {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
            account.freeze();
            accountRepository.save(account);
        });
    }

    @Override
    public void defreezeAccount(Long id) {
        self.requireAccountLock(id, () -> {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
            account.deFreeze();
            accountRepository.save(account);
        });
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
    }

    /**
     * 获取账户锁，并执行操作
     * 非预期内的失败重试
     * 这个账户锁和交易的锁并不互斥，在设计上我们禁止在AccountService内执行转账操作，所有和交易相关的逻辑由Transaction负责，因此不互斥并不会造成结果有致命的差异
     * 最坏的情况是，正在交易的过程中冻结了账户，但因为我们在交易中采用的是RR的隔离级别，因此只会有一笔正在进行的交易无法被取消
     * @param id 账户ID
     * @param runnable 要执行的操作
     */
    @Retryable(noRetryFor = DomainException.class)
    public void requireAccountLock(long id, Runnable runnable) {
        final var lock = redissonClient.getLock("account:" + id);
        try {
            if (lock.tryLock(10, 15, TimeUnit.SECONDS)) {
                try {
                    runnable.run();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.error("[{}]Error while acquiring lock for account: ", id, e);
            throw new RuntimeException(e);
        }
    }
}