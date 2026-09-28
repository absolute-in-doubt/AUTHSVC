package com.innowise.authservice.infrastructure.outbox.out;

import com.innowise.authservice.application.dto.CreateUserDto;
import org.springframework.http.*;

public interface UserServiceClient {

        ResponseEntity<Void> createUser(CreateUserDto requestDto);
}