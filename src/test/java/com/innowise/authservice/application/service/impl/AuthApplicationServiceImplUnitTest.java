package com.innowise.authservice.application.service.impl;


import com.innowise.authservice.application.dto.*;
import com.innowise.authservice.application.service.JwtService;
import com.innowise.authservice.domain.event.CreateUserEvent;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.model.Session;
import com.innowise.authservice.domain.model.UserCredentials;
import com.innowise.authservice.domain.model.UserStatus;
import com.innowise.authservice.domain.model.exception.*;
import com.innowise.authservice.domain.port.out.SessionRepository;
import com.innowise.authservice.domain.port.out.UserCredentialsRepository;
import com.innowise.authservice.infrastructure.security.service.HashManager;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceImplUnitTest {

    @Mock
    private HashManager hashManager;
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
        when(jwtService.createJwt(eq("1"), eq("testuser"), anyList(), any(LocalDateTime.class)))
                .thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("accessToken123");
        when(hashManager.hash(anyString())).thenReturn("refreshTokenHashValue");

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
        verify(hashManager).hash(anyString());
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
    void logIn_ShouldReturnTokens_WhenCredentialsValid() throws IncorrectLoginOrPasswordException, UserCreationPendingException {
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
        when(sessionRepository.findByIpAddressAndUserAgentAndActiveTrue(ipAddress, userAgent)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setSessionId(1L);
            s.setExpiresAt(LocalDateTime.now().plusHours(1));
            return s;
        });
        when(jwtService.createJwt(eq("1"), eq("testuser"), anyList(), any(LocalDateTime.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("accessToken123");
        when(hashManager.hash(anyString())).thenReturn("refreshTokenHashValue");

        // When
        TwoTokensResponseDto result = authService.logIn(request, ipAddress, userAgent);

        // Then
        assertNotNull(result);
        assertEquals("accessToken123", result.accessToken());
        assertNotNull(result.refreshToken());
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(hashManager).hash(anyString());
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
    void logIn_ShouldDeactivateExistingSession_WhenSameIpAndUserAgent() throws IncorrectLoginOrPasswordException, UserCreationPendingException {
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
                .status(UserStatus.ACTIVE)
                .build();

        when(userCredentialsRepository.findByLogin("testuser")).thenReturn(Optional.of(userCredentials));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(sessionRepository.findByIpAddressAndUserAgentAndActiveTrue(ipAddress, userAgent)).thenReturn(Optional.of(existingSession));
        when(hashManager.hash(anyString())).thenReturn("refreshTokenHashValue");
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            s.setSessionId(2L);
            s.setExpiresAt(LocalDateTime.now().plusHours(1));
            return s;
        });
        when(jwtService.createJwt(anyString(), anyString(), anyList(), any(LocalDateTime.class))).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("accessToken123");

        // When
        authService.logIn(request, ipAddress, userAgent);

        // Then
        verify(sessionRepository).setActive(1L, false);
        verify(hashManager).hash(anyString());
    }

    @Test
    void logIn_ShouldThrowException_WhenUserIsPending() throws IncorrectLoginOrPasswordException, UserCreationPendingException {
        // Given
        LogInRequestDto request = new LogInRequestDto("testuser", "password123");
        String ipAddress = "192.168.1.1";
        String userAgent = "Chrome";


        UserCredentials userCredentials = UserCredentials.builder()
                .userId(1L)
                .login("testuser")
                .passwordHash("encodedPassword")
                .roles(List.of(Role.USER))
                .status(UserStatus.PENDING)
                .build();

        when(userCredentialsRepository.findByLogin("testuser")).thenReturn(Optional.of(userCredentials));

        assertThrows(UserCreationPendingException.class,
                () -> authService.logIn(request, ipAddress, userAgent));

    }

    // Add to existing AuthApplicationServiceImplTest class

//    @Test
//    void refresh_ShouldReturnNewTokens_WhenRefreshTokenValid() throws ActiveSessionNotFoundException, UserCredentialsNotFoundException {
//        String oldRefreshToken = "oldToken";
//        String oldRefreshTokenHash = "hashedOldToken";
//        String newRefreshTokenHash = "hashedNewToken";
//
//        Session session = Session.builder()
//                .sessionId(1L)
//                .userId(1L)
//                .refreshTokenHash(oldRefreshTokenHash)
//                .active(true)
//                .expiresAt(LocalDateTime.now().plusHours(1))
//                .build();
//
//        UserCredentials userCredentials = UserCredentials.builder()
//                .userId(1L)
//                .login("testuser")
//                .roles(List.of(Role.USER))
//                .build();
//
//        when(passwordEncoder.encode(anyString()))
//                .thenReturn(oldRefreshTokenHash)
//                .thenReturn(newRefreshTokenHash);
//
//        when(sessionRepository.findByUserIdAndActiveTrue(oldRefreshTokenHash))
//                .thenReturn(Optional.of(session));
//        when(userCredentialsRepository.findById(1L)).thenReturn(Optional.of(userCredentials));
//        when(jwtService.createJwt(anyString(), anyString(), anyList(), any(LocalDateTime.class)))
//                .thenReturn(jwt);
//        when(jwt.getTokenValue()).thenReturn("newAccessToken");
//
//        TwoTokensResponseDto result = authService.refresh(oldRefreshToken);
//
//        assertEquals("newAccessToken", result.accessToken());
//        assertNotNull(result.refreshToken());
//        verify(sessionRepository).updateRefreshTokenHash(1L, newRefreshTokenHash);
//    }
//
//
//    @Test
//    void refresh_ShouldThrowException_WhenSessionNotFound() {
//        // Given
//        String refreshToken = "invalidToken";
//        String refreshTokenHash = "hashedInvalid";
//
//        when(passwordEncoder.encode(refreshToken)).thenReturn(refreshTokenHash);
//        when(sessionRepository.findByRefreshTokenHashAndActiveTrue(refreshTokenHash))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ActiveSessionNotFoundException.class,
//                () -> authService.refresh(refreshToken));
//    }
//
//
//    @Test
//    void refresh_ShouldThrowException_WhenUserCredentialsNotFound() {
//        // Given
//        String refreshToken = "validToken";
//        String refreshTokenHash = "hashedValid";
//        Session session = Session.builder().sessionId(1L).userId(999L).build();
//
//        when(passwordEncoder.encode(refreshToken)).thenReturn(refreshTokenHash);
//        when(sessionRepository.findByRefreshTokenHashAndActiveTrue(refreshTokenHash))
//                .thenReturn(Optional.of(session));
//        when(userCredentialsRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(UserCredentialsNotFoundException.class,
//                () -> authService.refresh(refreshToken));
//    }


    @Test
    void logOut_ShouldDeactivateAllUserSessions() {
        // When
        authService.logOut(1L);

        // Then
        verify(sessionRepository).setActiveFalseByUserId(1L);
    }

    @Test
    void deactivateSessionById_ShouldDeactivateSession() {
        // When
        authService.deactivateSessionById(1L);

        // Then
        verify(sessionRepository).setActive(1L, false);
    }

    @Test
    void registerAdmin_ShouldCreateAdminUser() throws LoginIsAlreadyTakenException {
        // Given
        RegisterRequestDto request = new RegisterRequestDto(
                "admin", "admin123", "Admin", "User",
                LocalDate.of(1990, 1, 1), "admin@example.com"
        );

        when(passwordEncoder.encode("admin123")).thenReturn("encodedAdmin");
        when(userCredentialsRepository.existsByLogin("admin")).thenReturn(false);
        when(userCredentialsRepository.save(any(UserCredentials.class))).thenAnswer(inv -> {
            UserCredentials uc = inv.getArgument(0);
            uc.setUserId(1L);
            return uc;
        });

        // When
        authService.registerAdmin(request);

        // Then
        verify(userCredentialsRepository).save(argThat(uc ->
                uc.getLogin().equals("admin") &&
                        uc.getPasswordHash().equals("encodedAdmin") &&
                        uc.getRoles().equals(List.of(Role.ADMIN)) &&
                        uc.getStatus() == UserStatus.PENDING
        ));
        verify(eventPublisher).publishEvent((Object) argThat(e ->
                e instanceof CreateUserEvent ce && ce.userId().equals(1L)
        ));

    }

    @Test
    void registerAdmin_ShouldThrowException_WhenLoginTaken() {
        // Given
        RegisterRequestDto request = new RegisterRequestDto(
                "admin", "admin123", "Admin", "User",
                LocalDate.of(1990, 1, 1), "admin@example.com"
        );

        when(userCredentialsRepository.existsByLogin("admin")).thenReturn(true);

        // When & Then
        assertThrows(LoginIsAlreadyTakenException.class,
                () -> authService.registerAdmin(request));
    }

    @Test
    void activateUserCredentials_ShouldSetUserActive() {
        // When
        authService.activateUserCredentials(1L);

        // Then
        verify(userCredentialsRepository).setStatusByUserId(1L, UserStatus.ACTIVE);
    }

    @Test
    void deactivateUserCredentials_ShouldSetUserInactive() {
        // When
        authService.deactivateUserCredentials(1L);

        // Then
        verify(userCredentialsRepository).setStatusByUserId(1L, UserStatus.DEACTIVATED);
    }
}