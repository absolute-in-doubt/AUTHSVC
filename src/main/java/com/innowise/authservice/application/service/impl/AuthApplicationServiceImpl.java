package com.innowise.authservice.application.service.impl;

import com.innowise.authservice.application.dto.*;
import com.innowise.authservice.application.service.AuthApplicationService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final HashManager hashManager;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialsRepository userCredentialsRepository;
    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtService jwtService;

    @Override
    @Transactional
    public TwoTokensResponseDto register(RegisterRequestDto requestDto, String ipAddress, String userAgent)
            throws LoginIsAlreadyTakenException {

        List<Role> roles = List.of(Role.USER);
        String refreshToken = UUID.randomUUID().toString();

        UserCredentials userCredentials = UserCredentials.builder()
                .login(requestDto.login())
                .passwordHash(passwordEncoder.encode(requestDto.password()))
                .roles(roles)
                .status(UserStatus.PENDING)
                .build();

        if(userCredentialsRepository.existsByLogin(requestDto.login()))
            throw new LoginIsAlreadyTakenException(requestDto.login());

        userCredentials = userCredentialsRepository.save(userCredentials);



        Session session = Session.builder()    //NOTE: expiresAt is set in ExpiresAtSessionEventListener
                .userId(userCredentials.getUserId())
                .refreshTokenHash(hashManager.hash(refreshToken))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .active(true)
                .build();

        sessionRepository.save(session);

        eventPublisher.publishEvent(new CreateUserEvent(
                userCredentials.getUserId(),
                requestDto.firstName(),
                requestDto.lastName(),
                requestDto.birthDate(),
                requestDto.email()
        ));

        String accessToken = jwtService.createJwt(
                userCredentials.getUserId(),
                requestDto.login(),
                roles,
                session.getExpiresAt()
                ).getTokenValue();

        return new TwoTokensResponseDto(accessToken, refreshToken);
    }

    @Transactional
    @Override
    public TwoTokensResponseDto logIn(LogInRequestDto logInRequestDto, String ipAddress, String userAgent) throws IncorrectLoginOrPasswordException, UserCreationPendingException {


        String refreshToken = UUID.randomUUID().toString();

        UserCredentials userCredentials = userCredentialsRepository.findByLogin(logInRequestDto.login())
                .orElseThrow(() ->  new IncorrectLoginOrPasswordException(logInRequestDto.login()));

        if(userCredentials.getStatus().equals(UserStatus.PENDING))
            throw new UserCreationPendingException(userCredentials.getUserId());

        if(!passwordEncoder.matches(logInRequestDto.password(), userCredentials.getPasswordHash()))
            throw new IncorrectLoginOrPasswordException(logInRequestDto.login());

        sessionRepository.findByIpAddressAndUserAgentAndActiveTrue(ipAddress, userAgent)
                .ifPresent(existingSession -> sessionRepository.setActive(existingSession.getSessionId(), false));


        Session session = Session.builder()    //NOTE: expiresAt is set in ExpiresAtSessionEventListener
                .userId(userCredentials.getUserId())
                .refreshTokenHash(hashManager.hash(refreshToken))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .active(true)
                .build();

        sessionRepository.save(session);

        String accessToken = jwtService.createJwt(
                userCredentials.getUserId(),
                logInRequestDto.login(),
                userCredentials.getRoles(),
                session.getExpiresAt()
        ).getTokenValue();

        return new TwoTokensResponseDto(accessToken, refreshToken);
    }

    @Transactional
    @Override
    public TwoTokensResponseDto refresh(String refreshToken) throws ActiveSessionNotFoundException {
        String refreshTokenHash = hashManager.hash(refreshToken);

        Session session = sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> new ActiveSessionNotFoundException(refreshTokenHash));

        UserCredentials userCredentials = userCredentialsRepository.findById(session.getUserId())
                .orElseThrow(() -> new UserCredentialsNotFoundException(session.getUserId()));

        String newRefreshToken = UUID.randomUUID().toString();
        sessionRepository.updateRefreshTokenHash(session.getSessionId(), passwordEncoder.encode(newRefreshToken));

        String accessToken = jwtService.createJwt(
                session.getUserId(),
                userCredentials.getLogin(),
                userCredentials.getRoles(),
                session.getExpiresAt()
        ).getTokenValue();

        return new TwoTokensResponseDto(accessToken, newRefreshToken);
    }


    @Transactional
    @Override
    public void logOut(Long userId) {
        sessionRepository.setActiveFalseByUserId(userId);
    }

    @Transactional
    @Override
    public void deactivateSessionById(Long sessionId) {
        sessionRepository.setActive(sessionId, false);
    }

    @Override
    @Transactional
    public void registerAdmin(RegisterRequestDto registerRequestDto) throws LoginIsAlreadyTakenException {
        List<Role> roles = List.of(Role.ADMIN);

        UserCredentials userCredentials = UserCredentials.builder()
                .login(registerRequestDto.login())
                .passwordHash(passwordEncoder.encode(registerRequestDto.password()))
                .roles(roles)
                .status(UserStatus.PENDING)
                .build();

        if(userCredentialsRepository.existsByLogin(registerRequestDto.login()))
            throw new LoginIsAlreadyTakenException(registerRequestDto.login());

        userCredentials = userCredentialsRepository.save(userCredentials);

        eventPublisher.publishEvent(new CreateUserEvent(
                userCredentials.getUserId(),
                registerRequestDto.firstName(),
                registerRequestDto.lastName(),
                registerRequestDto.birthDate(),
                registerRequestDto.email()
        ));
    }

    @Transactional
    @Override
    public void activateUserCredentials(Long userId) {
        userCredentialsRepository.setStatusByUserId(userId, UserStatus.ACTIVE);
    }

    @Override
    public void deactivateUserCredentials(Long userId) {
        userCredentialsRepository.setStatusByUserId(userId, UserStatus.DEACTIVATED);
    }
}
