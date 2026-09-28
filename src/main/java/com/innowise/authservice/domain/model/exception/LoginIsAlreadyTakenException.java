package com.innowise.authservice.domain.model.exception;

public class LoginIsAlreadyTakenException extends Exception {
    public LoginIsAlreadyTakenException(String login) {
        super("Login <" + login + "> is already taken");
    }
}
