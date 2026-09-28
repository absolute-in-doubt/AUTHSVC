package com.innowise.authservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogInRequestDto(
        @NotBlank @Size(min = 5,max = 20) String login,
        @NotBlank @Size(min = 6,max = 35) String password
) {
}
