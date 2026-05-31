package com.innowise.authservice.application.service;


import com.innowise.authservice.application.dto.AccessTokenResponseDto;
import com.innowise.authservice.application.dto.ServiceAuthenticationRequestDto;
import com.innowise.authservice.domain.model.exception.IncorrectServiceCredentialsException;

/*
Service that handles the authentication of other services
 */
public interface ServiceAuthApplicationService {

    AccessTokenResponseDto authenticate(ServiceAuthenticationRequestDto requestDto) throws IncorrectServiceCredentialsException;

}
