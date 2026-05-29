package com.innowise.authservice.application.service.impl;

import com.innowise.authservice.application.dto.*;
import com.innowise.authservice.application.service.AuthApplicationService;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.model.UserCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public TwoTokensResponseDto register(RegisterRequestDto requestDto, String ipAddress, String userAgent) {

        CreateUserDto createUserDto = new CreateUserDto(
                requestDto.firstName(),
                requestDto.lastName(),
                requestDto.birthDate(),
                requestDto.email()
        );


        /*
        TODO create a UserCredentials Status {PENDING, ACTIVE, DEACTIVATED, FAILED_TO_ACTIVATE}
        TODO make user service return the same response on the duplicate request to create the same user
         (but without the actual creation of the duplicate)
        TODO create Outbox table (CreateUserDto, retries counter, status{UNPROCESSED, COMPLETED, DEAD_LETTER})
         */

        /*
        1. Create UserCredentials with status PENDING
        2. Create Session
        3. Persist UserCredentials and Session entities (User id is generated here)
        4. Create CreateUserDto with the userId
        2. Save CreateUserDto to the outbox table
        6. form TwoTokensResponse
         */

        /*
        Http retry worker:
        1. send requests to create user on user service
        2. if (duplicate requests arrive) userId won't let to create duplicate users) <- idempotency key
        3. as the success response received -> set UserCredentials status as ACTIVE
        4. if after several retries (in one transaction) we haven't received the successful response,
        increment the outbox event's retries counter
        if (the counter is greater than some threshold) mark the outbox event as DEAD_LETTER and the UserCredentials as FAILED_TO_ACTIVATE
         */

        UserCredentials userCredentials = UserCredentials.builder()
                .login(requestDto.login())
                .passwordHash(passwordEncoder.encode(requestDto.password()))
                .roles(List.of(Role.USER))
                .active(true)
                .build();


        return null;
    }

    @Override
    public TwoTokensResponseDto logIn(LogInRequestDto logInRequestDto) {
        return null;
    }

    @Override
    public AccessTokenResponseDto refresh(RefreshRequestDto refreshRequestDto) {
        return null;
    }

    @Override
    public AccessTokenResponseDto authenticate(ServiceAuthenticationRequestDto requestDto) {
        return null;
    }

    @Override
    public void logOut(Long userId) {

    }

    @Override
    public void deactivateSessionById(Long sessionId) {

    }

    @Override
    public void registerAdmin(RegisterRequestDto registerRequestDto) {

    }

    @Override
    public void activateUserCredentials(Long userId) {

    }

    @Override
    public void deactivateUserCredentials(Long userId) {

    }
}
