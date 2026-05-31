package com.innowise.authservice.infrastructure.outbox.out;

import com.innowise.authservice.application.dto.CreateUserDto;
import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.model.Role;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    @Value("${application.userservice.createUserUri}")
    private String createUserUri;

    private static final String AUTH_SERVICE_ID = "auth-service";
    public static final String USER_SERVICE = "user-service";
    private static final AtomicReference<String> jwt = new AtomicReference<>();

    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    @PostConstruct
    public void updateJwt(){
        //session expiresAt is set to one year, so that jwt is lifetime isn't limited by session ending
        jwt.set(jwtService.createJwt(
                        AUTH_SERVICE_ID,
                        AUTH_SERVICE_ID,
                        List.of(Role.SERVICE),
                        LocalDateTime.now().plusYears(1))
                .getTokenValue());
    }

    @CircuitBreaker(name = USER_SERVICE)
    @Retry(name=USER_SERVICE)
    public ResponseEntity<Void> createUser(CreateUserDto requestDto){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + jwt.get());

        HttpEntity<CreateUserDto> request = new HttpEntity<>(requestDto, headers);

        ResponseEntity<Void> response = restTemplate.postForEntity(createUserUri, request, Void.class);

        if(response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)){
            updateJwt();
            response = restTemplate.postForEntity(createUserUri, request, Void.class);
        }

        return response;
    }
}
