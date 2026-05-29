package com.innowise.authservice.application.dto;

public record ServiceAuthenticationRequestDto(
        String clientId,
        String clientSecret
) {
}
