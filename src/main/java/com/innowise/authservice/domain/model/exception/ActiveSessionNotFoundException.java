package com.innowise.authservice.domain.model.exception;

public class ActiveSessionNotFoundException extends Exception {
    public ActiveSessionNotFoundException(String refreshTokenHash) {
        super("Failed to find an active session with matching refresh token for user with refreshTokenHash {}; <" + refreshTokenHash + ">");
    }
}
