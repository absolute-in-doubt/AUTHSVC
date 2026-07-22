package com.innowise.authservice.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.authservice.application.security.service.DeviceDetailsResolver;
import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.infrastructure.security.filter.JwtAuthenticationFilter;
import com.innowise.authservice.infrastructure.security.service.HashManager;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Base64URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties properties;


    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   @Qualifier("customAuthenticationEntryPoint") AuthenticationEntryPoint authEntryPoint){
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(Customizer.withDefaults())
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
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
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint)
                        );
        return http.build();
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
    public PathMatcher pathMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            DeviceDetailsResolver deviceDetailsResolver,
            JwtService jwtService,
            PathMatcher pathMatcher,
            @Qualifier("customAuthenticationEntryPoint") AuthenticationEntryPoint authEntryPoint
    ){
        return new JwtAuthenticationFilter(
                authEntryPoint,
                new DefaultBearerTokenResolver(),
                deviceDetailsResolver,
                jwtService,
                pathMatcher,
                properties.paths().publicPaths()
        );
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

    @Bean
    public PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public List<SecurityProperties.Service> serviceCredentials(){
        return properties.services();
    }

    @Bean
    public HashManager hashManager() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                properties.hmacSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(keySpec);

        return (String value) ->
            HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
