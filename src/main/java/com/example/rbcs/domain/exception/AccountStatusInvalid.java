package com.example.rbcs.domain.exception;

public class AccountStatusInvalid extends DomainException {
    public AccountStatusInvalid(String message) {
        super(message);
    }
    
    public AccountStatusInvalid(String message, Throwable cause) {
        super(message, cause);
    }
} 