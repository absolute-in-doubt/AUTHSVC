package com.innowise.authservice.domain.port.in;

import com.innowise.authservice.application.dto.LogInRequestDto;
import com.innowise.authservice.application.dto.RegisterRequestDto;
import com.innowise.authservice.application.dto.TwoTokensResponseDto;
import com.innowise.authservice.domain.model.exception.ActiveSessionNotFoundException;
import com.innowise.authservice.domain.model.exception.IncorrectLoginOrPasswordException;
import com.innowise.authservice.domain.model.exception.LoginIsAlreadyTakenException;
import com.innowise.authservice.domain.model.exception.UserCreationPendingException;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import org.springframework.http.ResponseEntity;

public interface UserAuthController {

    ResponseEntity<TwoTokensResponseDto> register(JwtAuthenticationToken authentication, RegisterRequestDto requestDto) throws LoginIsAlreadyTakenException;

    ResponseEntity<TwoTokensResponseDto> logIn(JwtAuthenticationToken authentication, LogInRequestDto logInRequestDto) throws UserCreationPendingException, IncorrectLoginOrPasswordException;

    ResponseEntity<TwoTokensResponseDto> refresh(JwtAuthenticationToken authentication, String refreshToken) throws ActiveSessionNotFoundException;

    ResponseEntity<Void> logOut(JwtAuthenticationToken authentication);

    ResponseEntity<Void> deactivateSessionById(Long sessionId);

    ResponseEntity<Void> registerAdmin(RegisterRequestDto requestDto) throws LoginIsAlreadyTakenException;

//  I don't have that much time rn
//    ResponseEntity<Void> activateUserCredentials(Long userId);
//
//    ResponseEntity<Void> deactivateUserCredentials(Long userId);
}
