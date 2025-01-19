package com.example.rbcs.domain.exception;

public class AccountNotFoundException extends DomainException {
    public AccountNotFoundException() {
        super();
    }
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
} 