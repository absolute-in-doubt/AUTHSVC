package com.innowise.authservice;

import com.innowise.authservice.application.dto.RegisterRequestDto;
import com.innowise.authservice.application.service.AuthApplicationService;
import com.innowise.authservice.domain.model.Role;
import com.innowise.authservice.domain.model.UserCredentials;
import com.innowise.authservice.domain.port.out.UserCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
	> 2.3 Set up GlobalExceptionHandler
	3. implement integration tests
	4. implement e2e tests
	5. Review the transactional functionality


	User service:
	1. Implement basic security
	2. Implement outbox for the auth service user credentials activation/deactivation
	 */

	@Bean
	public ApplicationRunner adminInit(UserCredentialsRepository userCredentialsRepository, AuthApplicationService authApplicationService){
		return (args) -> {

			if(userCredentialsRepository.findAll().stream().noneMatch(uc -> uc.getRoles().contains(Role.ADMIN))){
				authApplicationService.registerAdmin(new RegisterRequestDto(
						"admin",
						"root_passwd",
						"admin",
						"admin",
						LocalDate.now().minus(20, ChronoUnit.YEARS),
						"admin@gmail.com"
				));
			}
		};
	}

	/*
	    @NotBlank @Size(min = 5,max = 20) String login,
        @NotBlank @Size(min = 6,max = 35) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Past LocalDate birthDate,
        @NotBlank @Email String email
	 */
}
