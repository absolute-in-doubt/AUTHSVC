package com.innowise.authservice.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "application.security")
public record SecurityProperties(
        //session length is injected via @Value
        Paths paths,
        List<JwkKey> jwks
        ) {


    public record Paths(
            Set<String> publicPaths,
            Set<String> userPaths,
            Set<String> adminPaths){}

    public record JwkKey(
            String kid,
            String kty,
            String alg,
            String use,
            String n,
            String e,
            String d,
            String p,
            String q,
            String dp,
            String dq,
            String qi
    ) {}
}
