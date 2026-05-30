package com.innowise.authservice.application.service;

import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.LocalDateTime;
import java.util.List;

public interface JwtService {

    Jwt createJwt(String userId, String login, List<Role> roles, LocalDateTime sessionExpiresAt);

    Jwt decode(String jwt);

    boolean isTokenValid(Jwt jwt);

    JwtAuthenticationToken convert(Jwt jwt);
}
