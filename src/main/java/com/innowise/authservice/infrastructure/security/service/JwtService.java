package com.innowise.authservice.infrastructure.security.service;

import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import com.innowise.authservice.domain.security.model.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    private static class JwtClaim {
        static String LOGIN = "login";
        static String ROLES = "roles";
    }


    public Jwt createJwt(){
        //TODO pass them somehow as arguments
        Long userId = 0L;
        String login = "";
        Collection<GrantedAuthority> authorities = null;
        Instant expiresAt = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim(JwtClaim.LOGIN, login)
                .claim(JwtClaim.ROLES, authorities)
                .expiresAt(expiresAt)
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
