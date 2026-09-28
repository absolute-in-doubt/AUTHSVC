package com.innowise.authservice.infrastructure.security.model;


import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import org.jspecify.annotations.Nullable;

public interface AuthenticationContext {

    Object getPrincipal();

    @Nullable
    DeviceAuthenticationDetails getDetails();
}
