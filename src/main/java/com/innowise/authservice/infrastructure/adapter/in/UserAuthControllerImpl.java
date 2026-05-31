package com.innowise.authservice.infrastructure.adapter.in;

import com.innowise.authservice.application.dto.LogInRequestDto;
import com.innowise.authservice.application.dto.RegisterRequestDto;
import com.innowise.authservice.application.dto.TwoTokensResponseDto;
import com.innowise.authservice.application.service.AuthApplicationService;
import com.innowise.authservice.domain.model.exception.ActiveSessionNotFoundException;
import com.innowise.authservice.domain.model.exception.IncorrectLoginOrPasswordException;
import com.innowise.authservice.domain.model.exception.LoginIsAlreadyTakenException;
import com.innowise.authservice.domain.model.exception.UserCreationPendingException;
import com.innowise.authservice.domain.port.in.UserAuthController;
import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import com.innowise.authservice.domain.security.model.JwtUserDetails;
import com.innowise.authservice.infrastructure.security.annotation.CurrentAuthentication;
import com.innowise.authservice.infrastructure.security.model.JwtAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class UserAuthControllerImpl implements UserAuthController {

    private final AuthApplicationService userAuthService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<TwoTokensResponseDto> register(@CurrentAuthentication JwtAuthenticationToken authentication, @RequestBody RegisterRequestDto requestDto) throws LoginIsAlreadyTakenException {
        DeviceAuthenticationDetails deviceDetails = (DeviceAuthenticationDetails) authentication.getDetails();
        TwoTokensResponseDto response = userAuthService.register(requestDto, deviceDetails.ipAddress(), deviceDetails.userAgent());
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<TwoTokensResponseDto> logIn(@CurrentAuthentication JwtAuthenticationToken authentication, LogInRequestDto logInRequestDto) throws UserCreationPendingException, IncorrectLoginOrPasswordException {
        DeviceAuthenticationDetails deviceDetails = (DeviceAuthenticationDetails) authentication.getDetails();
        TwoTokensResponseDto response = userAuthService.logIn(logInRequestDto, deviceDetails.ipAddress(), deviceDetails.userAgent());
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/refresh")
    public ResponseEntity<TwoTokensResponseDto> refresh(@RequestParam("refreshToken") String refreshToken) throws ActiveSessionNotFoundException {
        return ResponseEntity.ok(userAuthService.refresh(refreshToken));
    }

    @Override
    @GetMapping("/logout")
    public ResponseEntity<Void> logOut(@CurrentAuthentication JwtAuthenticationToken authentication) {
        JwtUserDetails jwtUserDetails = (JwtUserDetails) authentication.getPrincipal();
        userAuthService.logOut(jwtUserDetails.userId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Override
    @PatchMapping("/session/{sessionId}/deactivate")
    public ResponseEntity<Void> deactivateSessionById(@PathVariable("sessionId") Long sessionId) {
        userAuthService.deactivateSessionById(sessionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Override
    @PostMapping("/admin/register")
    public ResponseEntity<Void> registerAdmin(RegisterRequestDto requestDto) throws LoginIsAlreadyTakenException {
        userAuthService.registerAdmin(requestDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
