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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Defers defers;
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

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Retryable(maxAttempts = 3)
    public void executeTransaction(long transactionId) {
        final var transaction = transactionRepository.findById(transactionId).orElseThrow(TransactionNotFoundException::new);
        try {
            transaction.execute();
            defers.deferOnTransactionComplete(() -> {
                log.info("[{}]Transaction Completed: {}", transaction.getId(), transaction);
                eventPublisher.publishEvent(new TransactionCompletedEvent(transaction));
            });
        } catch (DomainException e) {
            transaction.fail(e.getMessage());
            defers.deferOnTransactionComplete(() -> {
                log.info("[{}]Transaction Completed: {}", transaction.getId(), transaction);
                eventPublisher.publishEvent(new TransactionFailedEvent(transaction));
            });
        }
        transactionRepository.save(transaction);
    }
}
