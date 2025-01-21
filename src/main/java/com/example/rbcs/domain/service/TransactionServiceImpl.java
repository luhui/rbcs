package com.example.rbcs.domain.service;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.entity.Transaction;
import com.example.rbcs.domain.event.TransactionCompletedEvent;
import com.example.rbcs.domain.event.TransactionCreatedEvent;
import com.example.rbcs.domain.event.TransactionFailedEvent;
import com.example.rbcs.domain.exception.AmountInvalidException;
import com.example.rbcs.domain.exception.DomainException;
import com.example.rbcs.domain.exception.TransactionAccountInvalid;
import com.example.rbcs.domain.exception.TransactionNotFoundException;
import com.example.rbcs.domain.repository.TransactionRepository;
import com.example.rbcs.infrastructure.defer.Defers;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Defers defers;
    private final RedissonClient redissonClient;
    @Resource
    @Lazy
    private TransactionServiceImpl self;
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Transaction createTransaction(Account source, Account target, Transaction.Type type, long amount) {
        if (source == null) {
            throw new TransactionAccountInvalid("Source accounts cannot be null");
        }
        if (type == null) {
            throw new TransactionAccountInvalid("Transaction type cannot be null");
        }
        if (type == Transaction.Type.TRANSFER) {
            if (target == null) {
                throw new TransactionAccountInvalid("Target accounts cannot be null");
            }
            if (Objects.equals(source.getId(), target.getId())) {
                throw new TransactionAccountInvalid("Source and target accounts cannot be the same");
            }
        }
        if (amount <= 0) {
            throw new AmountInvalidException("Amount must be greater than zero");
        }
        log.info("Creating transaction for source account: {}, target account: {}", source.getId(), target != null ? target.getId() : "None");
        Transaction transaction = Transaction.builder()
                .sourceAccount(source)
                .targetAccount(target)
                .type(type)
                .amount(amount)
                .build();
        transactionRepository.save(transaction);

        defers.deferOnTransactionComplete(() -> {
            log.info("[{}]Transaction Created: {}", transaction.getId(), transaction);
            eventPublisher.publishEvent(new TransactionCreatedEvent(transaction));
        });
        return transaction;
    }

    /**
     * 执行交易
     * 1. 先对被交易对象加锁
     * 2. 取得锁后，开启数据库事务，隔离级别REPEATABLE_READ，避免幻读
     * 3. 如果处理失败在预期的异常（DomainException），标记交易失败，否则重试3次
     * @param transactionId 交易ID
     */
    @Retryable(maxAttempts = 3)
    public void executeTransaction(long transactionId) {
        final var transaction = transactionRepository.findById(transactionId).orElseThrow(TransactionNotFoundException::new);
        final var source = transaction.getSourceAccount();
        final var target = transaction.getTargetAccount();
        final var sourceLock =redissonClient.getLock("t:a:" + source.getId());
        var lock = sourceLock;
        if (target != null) {
            final var targetLock = redissonClient.getLock("t:a:" + target.getId());
            lock = redissonClient.getMultiLock(sourceLock, targetLock);
        }
        try {
            if (lock.tryLock(100, 300, TimeUnit.SECONDS)) {
                try {
                    self.doExecuteTransaction(transactionId);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            log.info("[{}]unable to acquire lock", transactionId);
            throw new RuntimeException(e);
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    @Retryable(maxAttempts = 3)
    public void doExecuteTransaction(long transactionId) {
        final var transaction = transactionRepository.findById(transactionId).orElseThrow(TransactionNotFoundException::new);
        try {
            transaction.execute();
            defers.deferOnTransactionComplete(() -> {
                log.info("[{}]Transaction Completed: {}", transaction.getId(), transaction);
                eventPublisher.publishEvent(new TransactionCompletedEvent(transaction));
            });
        } catch (DomainException e) {
            log.error("[{}]Transaction Failed", transaction.getId(), e);
            transaction.fail(e.getMessage());
            defers.deferOnTransactionComplete(() -> {
                eventPublisher.publishEvent(new TransactionFailedEvent(transaction));
            });
        }
        transactionRepository.save(transaction);
    }
}
