package com.innowise.authservice.domain.model.exception;

public class ActiveSessionNotFoundException extends Exception {
    public ActiveSessionNotFoundException(Long userId) {
        super("Failed to find an active session with matching refresh token for user with Id {}; <" + userId + ">");
    }
}
