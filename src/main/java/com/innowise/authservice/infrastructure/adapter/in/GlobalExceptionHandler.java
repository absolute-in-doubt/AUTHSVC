package com.innowise.authservice.infrastructure.adapter.in;

import com.innowise.authservice.domain.model.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoginIsAlreadyTakenException.class)
    public ResponseEntity<Object> handleLoginIsAlreadyTakenException(LoginIsAlreadyTakenException ex) {
        log.warn("Login already taken: {}", ex.getMessage());
        return buildErrorResponse("Login is already taken", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserCreationPendingException.class)
    public ResponseEntity<Object> handleUserCreationPendingException(UserCreationPendingException ex) {
        log.warn("User creation pending: {}", ex.getMessage());
        return buildErrorResponse("User creation is still pending", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IncorrectLoginOrPasswordException.class)
    public ResponseEntity<Object> handleIncorrectLoginOrPasswordException(IncorrectLoginOrPasswordException ex) {
        log.warn("Incorrect login or password: {}", ex.getMessage());
        return buildErrorResponse("Incorrect login or password", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ActiveSessionNotFoundException.class)
    public ResponseEntity<Object> handleActiveSessionNotFoundException(ActiveSessionNotFoundException ex) {
        log.warn("Active session not found: {}", ex.getMessage());
        return buildErrorResponse("Session not found or expired", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IncorrectServiceCredentialsException.class)
    public ResponseEntity<Object> handleIncorrectServiceCredentialsException(IncorrectServiceCredentialsException ex) {
        log.warn("Incorrect service credentials: {}", ex.getMessage());
        return buildErrorResponse("Invalid service credentials", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse("Access denied", HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
