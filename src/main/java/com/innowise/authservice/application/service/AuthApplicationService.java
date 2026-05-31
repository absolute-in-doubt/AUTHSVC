package com.innowise.authservice.application.service;

import com.innowise.authservice.application.dto.*;
import com.innowise.authservice.domain.model.exception.ActiveSessionNotFoundException;
import com.innowise.authservice.domain.model.exception.IncorrectLoginOrPasswordException;
import com.innowise.authservice.domain.model.exception.LoginIsAlreadyTakenException;
import com.innowise.authservice.domain.model.exception.UserCreationPendingException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface AuthApplicationService {

    TwoTokensResponseDto register(RegisterRequestDto requestDto, String ipAddress, String userAgent) throws LoginIsAlreadyTakenException;

    TwoTokensResponseDto logIn(LogInRequestDto logInRequestDto, String ipAddress, String userAgent) throws IncorrectLoginOrPasswordException, UserCreationPendingException;

    TwoTokensResponseDto refresh(Long userId, String refreshToken) throws ActiveSessionNotFoundException;

    //User

    void logOut(Long userId);

    //Admin

    void deactivateSessionById(Long sessionId);

    //activating sessions is impossible as it'll make the expired sessions eternally alive

    void registerAdmin(RegisterRequestDto registerRequestDto) throws LoginIsAlreadyTakenException;



    //User service calls

    void activateUserCredentials(Long userId);

    void deactivateUserCredentials(Long userId);


}
