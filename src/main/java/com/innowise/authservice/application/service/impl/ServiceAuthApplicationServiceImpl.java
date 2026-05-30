package com.innowise.authservice.application.service.impl;

import com.innowise.authservice.application.dto.AccessTokenResponseDto;
import com.innowise.authservice.application.dto.ServiceAuthenticationRequestDto;
import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.application.service.ServiceAuthApplicationService;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.model.exception.IncorrectServiceCredentialsException;
import com.innowise.authservice.infrastructure.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAuthApplicationServiceImpl implements ServiceAuthApplicationService {

    private final List<SecurityProperties.Service> serviceCredentials;

    private final JwtService jwtService;

    @Value("${application.security.serviceJwtLifeMinutes}")
    private int serviceSessionLengthMinutes;

    @Override
    public AccessTokenResponseDto authenticate(ServiceAuthenticationRequestDto requestDto) throws IncorrectServiceCredentialsException {

        SecurityProperties.Service service = serviceCredentials.stream().filter(s -> requestDto.clientId().equals(s.clientId()))
                .findFirst().orElseThrow(() -> new IncorrectServiceCredentialsException(requestDto.clientId()));

        if(!requestDto.clientSecret().equals(service.clientSecret()))
            throw new IncorrectServiceCredentialsException(requestDto.clientId());

        String accessToken = jwtService.createJwt(
                requestDto.clientId(),
                requestDto.clientId(),
                List.of(Role.SERVICE),
                LocalDateTime.now().plusMinutes(serviceSessionLengthMinutes)
        ).getTokenValue();

        return new AccessTokenResponseDto(accessToken);
    }
}
