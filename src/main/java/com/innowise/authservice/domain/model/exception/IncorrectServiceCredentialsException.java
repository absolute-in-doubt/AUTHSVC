package com.innowise.authservice.domain.model.exception;

public class IncorrectServiceCredentialsException extends Exception {
    public IncorrectServiceCredentialsException(Long clientId) {
        super("Failed to authenticate service with clientId: <" + clientId + ">");
    }
}
