package com.example.rbcs.domain.exception;

public class TransactionAccountInvalid extends DomainException {
    public TransactionAccountInvalid(String message) {
        super(message);
    }

    public TransactionAccountInvalid(String message, Throwable cause) {
        super(message, cause);
    }
} 