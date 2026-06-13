package com.innowise.authservice.infrastructure.security.filter;

import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import com.innowise.authservice.infrastructure.security.exception.CustomAuthenticationEntryPoint;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import com.innowise.authservice.application.security.service.DeviceDetailsResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationEntryPoint authEntryPoint;

    private final BearerTokenResolver tokenResolver;

    private final DeviceDetailsResolver deviceDetailsResolver;

    private final JwtService jwtService;

    private final PathMatcher pathMatcher;

    private final Set<String> publicPathPatterns;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.debug("Received a request for {}", request.getRequestURI());

        DeviceAuthenticationDetails deviceDetails = deviceDetailsResolver.resolve(request);

        log.debug("Retrieved DeviceDetails: {}", deviceDetails);

        boolean isPublicPath = publicPathPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
        log.debug("Request to {} is public: {}", request.getRequestURI(), isPublicPath);

        if (isPublicPath) {
            SecurityContextHolder.getContext().setAuthentication(JwtAuthenticationToken.empty(deviceDetails));
            log.debug("Loaded JwtAuthenticationToken into the SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
        } else {
            String accessToken = tokenResolver.resolve(request);

            if(accessToken == null){
                log.debug("Access token not found");
                authEntryPoint.commence(request, response,
                        new InsufficientAuthenticationException("Failed to resolve the access token for private path: " + request.getRequestURI()));
            }

            Jwt jwt = jwtService.decode(accessToken);
            if(!jwtService.isTokenValid(jwt)){
                log.debug("Access token invalid");
                authEntryPoint.commence(request, response,
                        new BadCredentialsException("Failed to resolve the access token for private path: " + request.getRequestURI()));
            }

            JwtAuthenticationToken jwtAuthenticationToken = jwtService.convert(jwt);
            jwtAuthenticationToken.setDetails(deviceDetails);
            SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}
