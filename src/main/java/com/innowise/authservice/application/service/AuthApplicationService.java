package com.innowise.authservice.application.service;

import com.innowise.authservice.application.dto.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface AuthApplicationService {

    TwoTokensResponseDto register(RegisterRequestDto requestDto, String ipAddress, String userAgent);

    TwoTokensResponseDto logIn(LogInRequestDto logInRequestDto);

    AccessTokenResponseDto refresh(RefreshRequestDto refreshRequestDto);

    //void logOut()

    //Admin

    void deactivateSessionById(Long sessionId);

    void activateSessionById(Long sessionId);
}
