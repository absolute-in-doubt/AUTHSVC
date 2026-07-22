package com.innowise.authservice.infrastructure.security.config;

import com.innowise.authservice.infrastructure.security.resolver.CurrentAuthenticationArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentAuthenticationArgumentResolver currentAuthenticationArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAuthenticationArgumentResolver);
    }
}
