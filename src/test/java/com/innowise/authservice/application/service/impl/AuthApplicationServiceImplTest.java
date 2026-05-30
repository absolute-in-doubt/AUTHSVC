package com.innowise.authservice.application.service.impl;


import com.innowise.authservice.application.dto.*;
import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.event.CreateUserEvent;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.model.Session;
import com.innowise.authservice.domain.model.UserCredentials;
import com.innowise.authservice.domain.model.UserStatus;
import com.innowise.authservice.domain.model.exception.IncorrectLoginOrPasswordException;
import com.innowise.authservice.domain.model.exception.LoginIsAlreadyTakenException;
import com.innowise.authservice.domain.port.out.SessionRepository;
import com.innowise.authservice.domain.port.out.UserCredentialsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserCredentialsRepository userCredentialsRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private JwtService jwtService;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private AuthApplicationServiceImpl authService;

    @Test
    void register_ShouldCreateUserAndSessionAndReturnTokens() throws LoginIsAlreadyTakenException {
        // Given
        RegisterRequestDto request = new RegisterRequestDto(
                "testuser", "password123", "John", "Doe",
                LocalDate.of(1990, 1, 1), "test@example.com"
        );
        String ipAddress = "192.168.1.1";
        String userAgent = "Chrome";

        when(passwordEncoder.encode(anyString())).thenReturn("encodedValue");
        when(userCredentialsRepository.existsByLogin("testuser")).thenReturn(false);
        when(userCredentialsRepository.save(any(UserCredentials.class)))
                .thenAnswer(inv -> {
            UserCredentials uc = inv.getArgument(0);
            uc.setUserId(1L);
            return uc;
        });
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setSessionId(1L);
            s.setExpiresAt(LocalDateTime.now().plusHours(1));
            return s;
        });
        when(jwtService.createJwt(eq(1L), eq("testuser"), anyList(), any(LocalDateTime.class)))
                .thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("accessToken123");

        // When
        TwoTokensResponseDto result = authService.register(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        assertEquals("accessToken123", result.accessToken());
        assertNotNull(result.refreshToken());

        verify(userCredentialsRepository).save(argThat(uc ->
                uc.getLogin().equals("testuser") &&
                        uc.getPasswordHash().equals("encodedValue") &&
                        uc.getRoles().equals(List.of(Role.USER)) &&
                        uc.getStatus() == UserStatus.PENDING
        ));
        verify(sessionRepository).save(argThat(s ->
                s.getUserId().equals(1L) &&
                        s.getIpAddress().equals(ipAddress) &&
                        s.getUserAgent().equals(userAgent) &&
                        s.isActive()
        ));
        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof CreateUserEvent ce &&
                        ce.userId().equals(1L) &&
                        ce.firstName().equals("John") &&
                        ce.lastName().equals("Doe")
        ));
    }

    @Test
    void register_ShouldThrowException_WhenLoginTaken() {
        // Given
        RegisterRequestDto request = new RegisterRequestDto(
                "takenuser", "password123", "John", "Doe",
                LocalDate.of(1990, 1, 1), "test@example.com"
        );

        when(userCredentialsRepository.existsByLogin("takenuser")).thenReturn(true);

        // When & Then
        assertThrows(LoginIsAlreadyTakenException.class,
                () -> authService.register(request, "192.168.1.1", "Chrome"));
    }

    @Test
    void logIn_ShouldReturnTokens_WhenCredentialsValid() {
        // Given
        LogInRequestDto request = new LogInRequestDto("testuser", "password123");
        String ipAddress = "192.168.1.1";
        String userAgent = "Chrome";

        UserCredentials userCredentials = UserCredentials.builder()
                .userId(1L)
                .login("testuser")
                .passwordHash("encodedPassword")
                .roles(List.of(Role.USER))
                .status(UserStatus.ACTIVE)
                .build();

        when(userCredentialsRepository.findByLogin("testuser")).thenReturn(Optional.of(userCredentials));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(sessionRepository.findByIpAddressAndUserAgent(ipAddress, userAgent)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedRefreshToken");
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setSessionId(1L);
            s.setExpiresAt(LocalDateTime.now().plusHours(1));
            return s;
        });
        when(jwtService.createJwt(eq(1L), eq("testuser"), anyList(), any(LocalDateTime.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("accessToken123");

        // When
        TwoTokensResponseDto result = authService.logIn(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        assertEquals("accessToken123", result.accessToken());
        assertNotNull(result.refreshToken());
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }

    @Test
    void logIn_ShouldThrowException_WhenUserNotFound() {
        // Given
        LogInRequestDto request = new LogInRequestDto("nonexistent", "password123");

        when(userCredentialsRepository.findByLogin("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IncorrectLoginOrPasswordException.class,
                () -> authService.logIn(request, "192.168.1.1", "Chrome"));
    }

    @Test
    void logIn_ShouldDeactivateExistingSession_WhenSameIpAndUserAgent() {
        // Given
        LogInRequestDto request = new LogInRequestDto("testuser", "password123");
        String ipAddress = "192.168.1.1";
        String userAgent = "Chrome";

        Session existingSession = Session.builder()
                .sessionId(1L)
                .userId(1L)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .active(true)
                .build();

        UserCredentials userCredentials = UserCredentials.builder()
                .userId(1L)
                .login("testuser")
                .passwordHash("encodedPassword")
                .roles(List.of(Role.USER))
                .build();

        when(userCredentialsRepository.findByLogin("testuser")).thenReturn(Optional.of(userCredentials));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(sessionRepository.findByIpAddressAndUserAgent(ipAddress, userAgent)).thenReturn(Optional.of(existingSession));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedRefreshToken");
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setSessionId(2L);
            s.setExpiresAt(LocalDateTime.now().plusHours(1));
            return s;
        });
        when(jwtService.createJwt(anyLong(), anyString(), anyList(), any(LocalDateTime.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("accessToken123");

        // When
        authService.logIn(request, ipAddress, userAgent);

        // Then
        verify(sessionRepository).setActive(1L, false);
    }
}