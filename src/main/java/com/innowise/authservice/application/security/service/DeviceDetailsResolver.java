package com.innowise.authservice.application.security.service;

import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import jakarta.servlet.http.HttpServletRequest;


@FunctionalInterface
public interface DeviceDetailsResolver {

    DeviceAuthenticationDetails resolve(HttpServletRequest request);
}
