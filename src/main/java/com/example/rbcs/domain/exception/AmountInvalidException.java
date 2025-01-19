package com.example.rbcs.domain.exception;

public class AmountInvalidException extends DomainException{
    public AmountInvalidException() {
    }

    public AmountInvalidException(String message) {
        super(message);
    }
}
