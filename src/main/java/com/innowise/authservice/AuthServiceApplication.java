package com.innowise.authservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
		log.info("""
				
				     - JSON spec: GET http://localhost:8081/v3/api-docs
				     - YAML spec: GET http://localhost:8081/v3/api-docs.yaml
				     - Swagger UI: GET http://localhost:8081/swagger-ui.html
				""");
	}

	/*
	> 1. Create controllers
	> 2. write a dockerfile and compose.yaml
	2.1 Set up authorization annotations
	> 2.2 Set up liquibase
	2.3 Set up GlobalExceptionHandler
	3. implement integration tests
	4. implement e2e tests
	5. Review the transactional functionality


	User service:
	1. Implement basic security
	2. Implement outbox for the auth service user credentials activation/deactivation
	 */
}
