package com.innowise.authservice.infrastructure.security.filter;

import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import com.innowise.authservice.application.security.service.DeviceDetailsResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Set;

@Slf4j
//Configured in the SecurityConfig
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenResolver tokenResolver;

    private final DeviceDetailsResolver deviceDetailsResolver;

    private final JwtService jwtService;

    private final PathMatcher pathMatcher;

    private final Set<String> publicPathPatterns;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //1. create an unauthenticated JwtAuthenticationToken
        //2. Fill in the DeviceAuthenticationDetails
        //3. Load it in the Security context        <- needed for the device info retrieval
        //              while registering or logging user in to create a new session or to avoid creating multiple
        //              sessions for the same device and straight up log user in without real
        //4. check if the path is public, if so -> filterChain.doFilter() + return;
        //5. if path is not public, resolve Jwt via BearerTokenResolver
        //6. if Jwt == null -> 401 Unauthorized
        //7. if Jwt is present, call JwtService convert(Jwt, JwtAuthenticationToken) <- adds data to the token
        //8. filterChain.doFilter

        log.debug("Received a request for {}", request.getRequestURI());

        DeviceAuthenticationDetails deviceDetails = deviceDetailsResolver.resolve(request);

        boolean isPublicPath = publicPathPatterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
        log.debug("Request to {} is public: {}", request.getRequestURI(), isPublicPath);

        if (isPublicPath) {
            SecurityContextHolder.getContext().setAuthentication(JwtAuthenticationToken.empty(deviceDetails));
        } else {
            String accessToken = tokenResolver.resolve(request);

            if(accessToken == null){
                log.debug("Access token not found");
                throw new AccessDeniedException("Failed to resolve the access token for private path: " + request.getRequestURI());
            }

            Jwt jwt = jwtService.decode(accessToken);
            if(!jwtService.isTokenValid(jwt)){
                log.debug("Access token invalid");
                throw new AccessDeniedException("Failed to resolve the access token for private path: " + request.getRequestURI());
            }

            JwtAuthenticationToken jwtAuthenticationToken = jwtService.convert(jwt);
            jwtAuthenticationToken.setDetails(deviceDetails);
            SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}
