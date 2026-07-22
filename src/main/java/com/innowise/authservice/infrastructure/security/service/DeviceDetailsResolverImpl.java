package com.innowise.authservice.infrastructure.security.service;

import com.innowise.authservice.application.security.service.DeviceDetailsResolver;
import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class DeviceDetailsResolverImpl implements DeviceDetailsResolver {
    @Override
    public DeviceAuthenticationDetails resolve(HttpServletRequest request) {
        String ipAddress;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            ipAddress = forwardedFor.split(",")[0].trim();
        }
        ipAddress = request.getRemoteAddr();

        String userAgent = request.getHeader("User-Agent");

        return new DeviceAuthenticationDetails(ipAddress, userAgent);
    }
}
