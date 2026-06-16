package com.innowise.authservice.application.dto;

public record ServiceAuthenticationRequestDto(
        Long clientId,
        String clientLogin,
        String clientSecret
) {
}
