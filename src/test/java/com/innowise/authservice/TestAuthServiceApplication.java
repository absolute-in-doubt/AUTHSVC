package com.innowise.authservice;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TestAuthServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
