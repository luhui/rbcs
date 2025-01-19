package com.example.rbcs.domain.exception;

public class TransactionNotFoundException extends DomainException {
    public TransactionNotFoundException() {
        super();
    }
    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 