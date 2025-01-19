package com.example.rbcs.domain.event;

import com.example.rbcs.domain.entity.Transaction;

public class TransactionCompletedEvent extends DomainEvent<Transaction> {
    public TransactionCompletedEvent(Transaction aggregateRoot) {
        super(aggregateRoot);
    }
}
