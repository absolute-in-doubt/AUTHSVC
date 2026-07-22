package com.innowise.authservice.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateUserDto(
        Long userId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email) {
}
