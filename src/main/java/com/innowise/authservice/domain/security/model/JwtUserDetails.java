package com.innowise.authservice.domain.security.model;

public record JwtUserDetails(
        Long userId,
        String login
) {
}
