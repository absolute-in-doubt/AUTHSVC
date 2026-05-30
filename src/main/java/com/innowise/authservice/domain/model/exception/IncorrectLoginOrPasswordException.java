package com.innowise.authservice.domain.model.exception;

public class IncorrectLoginOrPasswordException extends Exception {
    public IncorrectLoginOrPasswordException(String login) {
        super("Incorrect login or password for log in attempt with login: <" + login + ">");
    }
}
