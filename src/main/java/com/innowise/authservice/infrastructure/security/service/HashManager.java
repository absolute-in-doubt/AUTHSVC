package com.innowise.authservice.infrastructure.security.service;

@FunctionalInterface
public interface HashManager {

    String hash(String value);
}
