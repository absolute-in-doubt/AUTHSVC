package com.innowise.autservice;

import org.springframework.boot.SpringApplication;

public class TestAutserviceApplication {

	public static void main(String[] args) {
		SpringApplication.from(AutserviceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
