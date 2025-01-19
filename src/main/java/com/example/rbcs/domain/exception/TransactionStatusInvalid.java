package com.example.rbcs.domain.exception;

public class TransactionStatusInvalid extends DomainException {
    public TransactionStatusInvalid(String message) {
        super(message);
    }

    public TransactionStatusInvalid(String message, Throwable cause) {
        super(message, cause);
    }
} 