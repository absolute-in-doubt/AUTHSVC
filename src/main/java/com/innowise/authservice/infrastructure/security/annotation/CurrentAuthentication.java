package com.innowise.authservice.infrastructure.security.annotation;

import org.springframework.security.core.Authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAuthentication {
    Class<? extends Authentication> type() default Authentication.class;
}
