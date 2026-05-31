package com.innowise.authservice.domain.port.in;

import com.innowise.authservice.application.dto.AccessTokenResponseDto;
import com.innowise.authservice.application.dto.ServiceAuthenticationRequestDto;
import com.innowise.authservice.domain.model.exception.IncorrectServiceCredentialsException;
import org.springframework.http.ResponseEntity;

public interface ServiceAuthController {

    ResponseEntity<AccessTokenResponseDto> authenticate(ServiceAuthenticationRequestDto requestDto) throws IncorrectServiceCredentialsException;

}
