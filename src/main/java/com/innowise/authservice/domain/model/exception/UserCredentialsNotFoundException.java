package com.innowise.authservice.domain.model.exception;

public class UserCredentialsNotFoundException extends RuntimeException {
    public UserCredentialsNotFoundException(Long userId) {
        super("Failed to find UserCredentials with userId: <" + userId + ">\n"
        + "Warning: this is not expected to happen in the normal service workflow");
    }
}
