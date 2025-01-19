package com.example.rbcs.domain.event;

import com.example.rbcs.domain.entity.Transaction;

public class TransactionCreatedEvent extends DomainEvent<Transaction> {
    public TransactionCreatedEvent(Transaction aggregateRoot) {
        super(aggregateRoot);
    }
}
