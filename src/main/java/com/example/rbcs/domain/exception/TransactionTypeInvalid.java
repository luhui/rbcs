package com.example.rbcs.domain.exception;

public class TransactionTypeInvalid extends DomainException {
    public TransactionTypeInvalid(String message) {
        super(message);
    }

    public TransactionTypeInvalid(String message, Throwable cause) {
        super(message, cause);
    }
} 