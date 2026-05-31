package com.innowise.authservice.application.service.impl;

import com.innowise.authservice.application.dto.AccessTokenResponseDto;
import com.innowise.authservice.application.dto.ServiceAuthenticationRequestDto;
import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.model.exception.IncorrectServiceCredentialsException;
import com.innowise.authservice.infrastructure.security.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceAuthApplicationServiceImplTest {

    @Mock
    private List<SecurityProperties.Service> serviceCredentials;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private ServiceAuthApplicationServiceImpl serviceAuthApplicationService;

    private static final String VALID_CLIENT_ID = "service-client";
    private static final String VALID_CLIENT_SECRET = "service-secret";
    private static final String INVALID_CLIENT_ID = "unknown-client";
    private static final String WRONG_CLIENT_SECRET = "wrong-secret";
    private static final String ACCESS_TOKEN = "access.token.value";
    private static final int SERVICE_SESSION_LENGTH_MINUTES = 60;

    @BeforeEach
    void setUp() throws Exception {
        Field sessionField = ServiceAuthApplicationServiceImpl.class.getDeclaredField("serviceSessionLengthMinutes");
        sessionField.setAccessible(true);
        sessionField.set(serviceAuthApplicationService, SERVICE_SESSION_LENGTH_MINUTES);
    }


    @Test
    void authenticate_whenValidCredentialsProvided_shouldReturnAccessToken() throws IncorrectServiceCredentialsException {
        ServiceAuthenticationRequestDto requestDto = new ServiceAuthenticationRequestDto(
                VALID_CLIENT_ID,
                VALID_CLIENT_SECRET
        );

        SecurityProperties.Service mockService = new SecurityProperties.Service(
                VALID_CLIENT_ID,
                VALID_CLIENT_SECRET
        );

        when(serviceCredentials.stream()).thenReturn(
                List.of(mockService).stream()
        );

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        when(jwtService.createJwt(
                eq(VALID_CLIENT_ID),
                eq(VALID_CLIENT_ID),
                anyList(),
                any(LocalDateTime.class)
        )).thenReturn(mockJwt);

        AccessTokenResponseDto result = serviceAuthApplicationService.authenticate(requestDto);

        assertNotNull(result);
        assertEquals(ACCESS_TOKEN, result.accessToken());
        verify(jwtService).createJwt(
                eq(VALID_CLIENT_ID),
                eq(VALID_CLIENT_ID),
                eq(List.of(Role.SERVICE)),
                any(LocalDateTime.class)
        );
    }


    @Test
    void authenticate_whenUnknownClientIdProvided_shouldThrowException() {
        // Arrange
        ServiceAuthenticationRequestDto requestDto = new ServiceAuthenticationRequestDto(
                INVALID_CLIENT_ID,
                VALID_CLIENT_SECRET
        );

        SecurityProperties.Service mockService = new SecurityProperties.Service(
                VALID_CLIENT_ID,
                VALID_CLIENT_SECRET
        );

        when(serviceCredentials.stream()).thenReturn(
                List.of(mockService).stream()
        );

        IncorrectServiceCredentialsException exception = assertThrows(
                IncorrectServiceCredentialsException.class,
                () -> serviceAuthApplicationService.authenticate(requestDto)
        );

        assertEquals(
                "Failed to authenticate service with clientId: <" + INVALID_CLIENT_ID + ">",
                exception.getMessage()
        );
        verify(jwtService, never()).createJwt(any(), any(), any(), any());
    }


    @Test
    void authenticate_whenIncorrectClientSecretProvided_shouldThrowException() {
        ServiceAuthenticationRequestDto requestDto = new ServiceAuthenticationRequestDto(
                VALID_CLIENT_ID,
                WRONG_CLIENT_SECRET
        );

        SecurityProperties.Service mockService = new SecurityProperties.Service(
                VALID_CLIENT_ID,
                VALID_CLIENT_SECRET
        );

        when(serviceCredentials.stream()).thenReturn(
                List.of(mockService).stream()
        );


        IncorrectServiceCredentialsException exception = assertThrows(
                IncorrectServiceCredentialsException.class,
                () -> serviceAuthApplicationService.authenticate(requestDto)
        );

        assertEquals(
                "Failed to authenticate service with clientId: <" + VALID_CLIENT_ID + ">",
                exception.getMessage()
        );
        verify(jwtService, never()).createJwt(any(), any(), any(), any());
    }


    @Test
    void authenticate_whenMultipleServicesExistAndCredentialsValid_shouldReturnAccessToken() throws IncorrectServiceCredentialsException {
        ServiceAuthenticationRequestDto requestDto = new ServiceAuthenticationRequestDto(
                VALID_CLIENT_ID,
                VALID_CLIENT_SECRET
        );

        SecurityProperties.Service firstService = new SecurityProperties.Service(
                "other-client",
                "other-secret"
        );
        SecurityProperties.Service secondService = new SecurityProperties.Service(
                VALID_CLIENT_ID,
                VALID_CLIENT_SECRET
        );

        when(serviceCredentials.stream()).thenReturn(
                List.of(firstService, secondService).stream()
        );

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        when(jwtService.createJwt(
                eq(VALID_CLIENT_ID),
                eq(VALID_CLIENT_ID),
                anyList(),
                any(LocalDateTime.class)
        )).thenReturn(mockJwt);

        AccessTokenResponseDto result = serviceAuthApplicationService.authenticate(requestDto);

        assertNotNull(result);
        assertEquals(ACCESS_TOKEN, result.accessToken());
        verify(jwtService).createJwt(
                eq(VALID_CLIENT_ID),
                eq(VALID_CLIENT_ID),
                eq(List.of(Role.SERVICE)),
                any(LocalDateTime.class)
        );
    }
}
