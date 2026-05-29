package com.innowise.authservice.infrastructure.security;

import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import com.innowise.authservice.application.security.service.DeviceDetailsResolver;
import com.innowise.authservice.infrastructure.security.filter.JwtAuthenticationFilter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Base64URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import java.text.ParseException;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties properties;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter){
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // отключает SessionManagementFilter
                )
                .headers(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                }))
                .csrf(Customizer.withDefaults())
                .logout(AbstractHttpConfigurer::disable)
                .addFilterAfter(jwtAuthenticationFilter, LogoutFilter.class)
                .requestCache(AbstractHttpConfigurer::disable)
                .servletApi(Customizer.withDefaults())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(properties.paths().publicPaths().toArray(String[]::new)).permitAll()
                        .requestMatchers(properties.paths().userPaths().toArray(String[]::new)).hasAnyAuthority(Role.ADMIN.toString(), Role.USER.toString())
                        .requestMatchers(properties.paths().adminPaths().toArray(String[]::new)).hasAuthority(Role.ADMIN.toString())
                        .anyRequest().denyAll() //Check it
                )
                .anonymous(Customizer.withDefaults())
                .exceptionHandling(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public BearerTokenResolver publicPathsBearerTokenResolver() {
        return new DefaultBearerTokenResolver();
    }

    @Bean
    public JWKSet jwkSet() {
        List<JWK> jwks = properties.jwks().stream()
                .map(key -> {
                    try {
                        RSAKey.Builder builder = new RSAKey.Builder(
                                new Base64URL(key.n()),
                                new Base64URL(key.e())
                        )
                                .keyID(key.kid())
                                .algorithm(JWSAlgorithm.parse(key.alg()))
                                .keyUse(KeyUse.parse(key.use()));

                        if (key.d() != null) {
                            builder.privateExponent(new Base64URL(key.d()));
                        }
                        if (key.p() != null && key.q() != null) {
                            builder.firstPrimeFactor(new Base64URL(key.p()));
                            builder.secondPrimeFactor(new Base64URL(key.q()));
                        }

                        if(key.qi() != null && key.dp() != null && key.dq() != null){
                            builder.firstCRTCoefficient(new Base64URL(key.qi()));
                            builder.firstFactorCRTExponent(new Base64URL(key.dp()));
                            builder.secondFactorCRTExponent(new Base64URL(key.dq()));
                        }

                        return (JWK) builder.build();
                    } catch (ParseException e) {
                        log.error("Failed to create JWKS: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                }).toList();
        return new JWKSet(jwks);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JWKSet jwkSet) {
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return NimbusJwtDecoder
                .withJwkSource(jwkSource)
                .build();
    }
}
