package com.innowise.authservice.infrastructure.security.filter;

import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import com.innowise.authservice.application.security.service.DeviceDetailsResolver;
import com.innowise.authservice.infrastructure.security.service.JwtServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenResolver tokenResolver;

    private final DeviceDetailsResolver deviceDetailsResolver;

    private final JwtServiceImpl jwtServiceImpl;

    @Value("${application.filter.path.public}")
    private Set<String> publicPaths;


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

        DeviceAuthenticationDetails deviceDetails = deviceDetailsResolver.resolve(request);


        if(publicPaths.contains(request.getRequestURI())){
            SecurityContextHolder.getContext().setAuthentication(JwtAuthenticationToken.empty(deviceDetails));
        } else {
            String accessToken = tokenResolver.resolve(request);

            if(accessToken == null){
                throw new AccessDeniedException("Failed to resolve the access token for private path: " + request.getRequestURI());
            }

            Jwt jwt = jwtServiceImpl.decode(accessToken);
            if(!jwtServiceImpl.isTokenValid(jwt)){
                throw new AccessDeniedException("Failed to resolve the access token for private path: " + request.getRequestURI());
            }

            JwtAuthenticationToken jwtAuthenticationToken = jwtServiceImpl.convert(jwt);
            jwtAuthenticationToken.setDetails(deviceDetails);
            SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}
