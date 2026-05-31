package com.innowise.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

	/*
	1. Create controllers
	2. write a dockerfile and compose.yaml
	3. implement integration tests
	4. implement e2e tests
	5. Review the transactional functionality


	User service:
	1. Implement basic security
	2. Implement outbox for the auth service user credentials activation/deactivation
	 */
}
