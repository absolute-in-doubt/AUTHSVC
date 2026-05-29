package com.innowise.authservice.infrastructure.security.service;

import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import com.innowise.authservice.domain.security.model.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    @Value("${application.security.jwtLifeMinutes}")
    private long jwtLifeMinutes;

    private static class JwtClaim {
        static String LOGIN = "login";
        static String ROLES = "roles";
    }


    public Jwt createJwt(Long userId, String login, List<Role> roles, LocalDateTime sessionExpiresAt){
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(Role::toString)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        LocalDateTime dirtyExpiresAt = LocalDateTime.now().plusMinutes(jwtLifeMinutes);
        LocalDateTime expiresAt = (dirtyExpiresAt.isBefore(sessionExpiresAt))? dirtyExpiresAt : sessionExpiresAt;


        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim(JwtClaim.LOGIN, login)
                .claim(JwtClaim.ROLES, authorities)
                .expiresAt(expiresAt.atZone(ZoneId.systemDefault()).toInstant())
                .build();

        return encoder.encode(JwtEncoderParameters.from(claimsSet));
    }

    public Jwt decode(String jwt){
        return decoder.decode(jwt);
    }

    public boolean isTokenValid(Jwt jwt){
        Instant expiresAt = jwt.getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public JwtAuthenticationToken convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(JwtClaim.ROLES);
        Collection<GrantedAuthority> authorities = (roles == null)? List.of() :
                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        String userId = jwt.getSubject();
        String login = jwt.getClaimAsString(JwtClaim.LOGIN);

        JwtUserDetails userDetails = new JwtUserDetails(Long.parseLong(userId), login);

        return JwtAuthenticationToken.authenticated(
                userDetails,
                authorities
        );
    }
}
