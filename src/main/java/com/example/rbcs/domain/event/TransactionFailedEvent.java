package com.example.rbcs.domain.event;

import com.example.rbcs.domain.entity.Transaction;

public class TransactionFailedEvent extends DomainEvent<Transaction> {
    public TransactionFailedEvent(Transaction aggregateRoot) {
        super(aggregateRoot);
    }
}
