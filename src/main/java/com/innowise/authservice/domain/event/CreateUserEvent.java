package com.innowise.authservice.domain.event;

import java.time.LocalDate;

public record CreateUserEvent(
        Long userId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String email
) {}
