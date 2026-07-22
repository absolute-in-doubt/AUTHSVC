package com.innowise.authservice.domain.security.model;

import jakarta.annotation.Nullable;

public record DeviceAuthenticationDetails(
        @Nullable String ipAddress,
        @Nullable String userAgent
) {
}
