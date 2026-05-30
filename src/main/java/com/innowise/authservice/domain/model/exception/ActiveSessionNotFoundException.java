package com.innowise.authservice.domain.model.exception;

public class ActiveSessionNotFoundException extends Exception {
    public ActiveSessionNotFoundException(String refreshTokenHash) {
        super("Failed to find an active session with refreshTokenHash; <" + refreshTokenHash + ">");
    }
}
