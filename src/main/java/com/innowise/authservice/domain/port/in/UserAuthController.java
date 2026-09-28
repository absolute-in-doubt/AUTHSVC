package com.innowise.authservice.domain.port.in;

import com.innowise.authservice.application.dto.LogInRequestDto;
import com.innowise.authservice.application.dto.RegisterRequestDto;
import com.innowise.authservice.application.dto.TwoTokensResponseDto;
import com.innowise.authservice.domain.model.exception.ActiveSessionNotFoundException;
import com.innowise.authservice.domain.model.exception.IncorrectLoginOrPasswordException;
import com.innowise.authservice.domain.model.exception.LoginIsAlreadyTakenException;
import com.innowise.authservice.domain.model.exception.UserCreationPendingException;
import com.innowise.authservice.infrastructure.security.model.AuthenticationContext;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import org.springframework.http.ResponseEntity;

public interface UserAuthController {

    ResponseEntity<TwoTokensResponseDto> register(AuthenticationContext authentication, RegisterRequestDto requestDto) throws LoginIsAlreadyTakenException;

    ResponseEntity<TwoTokensResponseDto> logIn(AuthenticationContext authentication, LogInRequestDto logInRequestDto) throws UserCreationPendingException, IncorrectLoginOrPasswordException;

    ResponseEntity<TwoTokensResponseDto> refresh(String refreshToken) throws ActiveSessionNotFoundException;

    ResponseEntity<Void> logOut(AuthenticationContext authentication);

    ResponseEntity<Void> deactivateSessionById(Long sessionId);

    ResponseEntity<Void> registerAdmin(RegisterRequestDto requestDto) throws LoginIsAlreadyTakenException;

    ResponseEntity<Void> validate();
}
