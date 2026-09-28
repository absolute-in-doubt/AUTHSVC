package com.innowise.authservice.infrastructure.adapter.in;

import com.innowise.authservice.application.dto.AccessTokenResponseDto;
import com.innowise.authservice.application.dto.ServiceAuthenticationRequestDto;
import com.innowise.authservice.application.service.ServiceAuthApplicationService;
import com.innowise.authservice.domain.model.exception.IncorrectServiceCredentialsException;
import com.innowise.authservice.domain.port.in.ServiceAuthController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/service")
@RequiredArgsConstructor
public class ServiceAuthControllerImpl implements ServiceAuthController {

    private final ServiceAuthApplicationService serviceAuthService;

    @Override
    @PostMapping("/authenticate")
    public ResponseEntity<AccessTokenResponseDto> authenticate(@RequestBody ServiceAuthenticationRequestDto requestDto) throws IncorrectServiceCredentialsException {
        return ResponseEntity.ok(serviceAuthService.authenticate(requestDto));
    }
}
