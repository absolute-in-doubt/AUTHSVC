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
import liquibase.license.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final PasswordEncoder passwordEncoder;
    private final UserCredentialsRepository userCredentialsRepository;
    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtService jwtService;

    /*
    TODO make user service return the same response on the duplicate request to create the same user
     (but without the actual creation of the duplicate)
    */

    /*
    TODO create a retry worker
    Http retry worker:
    1. send requests to create user on user service
    2. if (duplicate requests arrive) userId won't let to create duplicate users) <- idempotency key
    3. as the success response received -> set UserCredentials status as ACTIVE
    4. if after several retries (in one transaction) we haven't received the successful response,
    increment the outbox event's retries counter
    if (the counter is greater than some threshold) mark the outbox event as DEAD_LETTER and the UserCredentials as FAILED_TO_ACTIVATE
     */

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
                .refreshTokenHash(passwordEncoder.encode(refreshToken))
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

        List<Role> roles = List.of(Role.USER);
        String refreshToken = UUID.randomUUID().toString();

        UserCredentials userCredentials = userCredentialsRepository.findByLogin(logInRequestDto.login())
                .orElseThrow(() ->  new IncorrectLoginOrPasswordException(logInRequestDto.login()));

        if(userCredentials.getStatus().equals(UserStatus.PENDING))
            throw new UserCreationPendingException(userCredentials.getUserId());

        if(!passwordEncoder.matches(logInRequestDto.password(), userCredentials.getPasswordHash()))
            throw new IncorrectLoginOrPasswordException(logInRequestDto.login());

        sessionRepository.findByIpAddressAndUserAgent(ipAddress, userAgent)
                .ifPresent(existingSession -> sessionRepository.setActive(existingSession.getSessionId(), false));


        Session session = Session.builder()    //NOTE: expiresAt is set in ExpiresAtSessionEventListener
                .userId(userCredentials.getUserId())
                .refreshTokenHash(passwordEncoder.encode(refreshToken))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .active(true)
                .build();

        sessionRepository.save(session);

        String accessToken = jwtService.createJwt(
                userCredentials.getUserId(),
                logInRequestDto.login(),
                roles,
                session.getExpiresAt()
        ).getTokenValue();

        return new TwoTokensResponseDto(accessToken, refreshToken);
    }

    /*
    Creates a new refresh token each time a new access token is requested
     */
    @Transactional
    @Override
    public TwoTokensResponseDto refresh(RefreshRequestDto refreshRequestDto) throws ActiveSessionNotFoundException {

        String refreshTokenHash = passwordEncoder.encode(refreshRequestDto.refreshToken());

        Session session = sessionRepository.findByRefreshTokenHashAndActiveTrue(refreshTokenHash)
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


    @Override
    public void logOut(Long userId) {

    }

    @Override
    public void deactivateSessionById(Long sessionId) {

    }

    @Override
    public void registerAdmin(RegisterRequestDto registerRequestDto) {

    }

    @Override
    public void activateUserCredentials(Long userId) {

    }

    @Override
    public void deactivateUserCredentials(Long userId) {

    }
}
