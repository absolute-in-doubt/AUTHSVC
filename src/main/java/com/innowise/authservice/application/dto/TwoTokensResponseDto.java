package com.innowise.authservice.application.dto;

public record TwoTokensResponseDto(
        String accessToken,
        String refreshToken
) {}
