package com.innowise.authservice.domain.model.exception;

/*
Is thrown when attempting to interact with user, whose user info isn't persisted on the user service yet
 */
public class UserCreationPendingException extends Exception {
    public UserCreationPendingException(Long userId) {
        super("User info persistence isn't finished for user with userId: <" + userId + ">");
    }
}
