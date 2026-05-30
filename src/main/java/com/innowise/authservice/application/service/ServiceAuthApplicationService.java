package com.innowise.authservice.application.service;


import com.innowise.authservice.application.dto.AccessTokenResponseDto;
import com.innowise.authservice.application.dto.ServiceAuthenticationRequestDto;
import com.innowise.authservice.domain.model.exception.IncorrectServiceCredentialsException;

/*
Service that handles the authentication of other services
 */
public interface ServiceAuthApplicationService {

    AccessTokenResponseDto authenticate(ServiceAuthenticationRequestDto requestDto) throws IncorrectServiceCredentialsException;

    /*
    1. Implement ServiceAuthenticationService
    2. Implement HttpRetryWorker. Make it use ServiceAuthenticationService to get the Jwt on it's own
    3. Implement controllers
    4. Implement ControllerAdvice
     */
}
