package com.innowise.authservice.infrastructure.security.resolver;

import com.innowise.authservice.infrastructure.security.annotation.CurrentAuthentication;
import com.innowise.authservice.infrastructure.security.model.AuthenticationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class CurrentAuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAuthentication.class)
                && Authentication.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        CurrentAuthentication annotation = parameter.getParameterAnnotation(CurrentAuthentication.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Retrieved the following Authentication form the SecurityContext inside the CurrentAuthentication argument resolver: {}", authentication);
        
        Class<? extends Authentication> requiredType = annotation.type();
        if (requiredType != Authentication.class && !requiredType.isInstance(authentication)) {
            throw new IllegalStateException(
                    "CurrentAuthentication requires type " + requiredType.getName() + 
                    " but found " + (authentication != null ? authentication.getClass().getName() : "null")
            );
        }
        
        return authentication;
    }
}
