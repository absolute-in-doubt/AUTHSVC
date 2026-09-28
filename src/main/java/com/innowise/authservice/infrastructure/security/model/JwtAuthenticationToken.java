package com.innowise.authservice.infrastructure.security.model;

import com.innowise.authservice.domain.security.model.DeviceAuthenticationDetails;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken implements AuthenticationContext{

    private Object principal;

    public static JwtAuthenticationToken empty(DeviceAuthenticationDetails deviceAuthenticationDetails){
        return new JwtAuthenticationToken(null, null, deviceAuthenticationDetails, false);
    }

    public static JwtAuthenticationToken authenticated(
            Object jwtUserDetails,
            @Nullable Collection<? extends GrantedAuthority> authorities){
        return new JwtAuthenticationToken(jwtUserDetails, authorities, null, true);
    }


    private JwtAuthenticationToken(
            Object principal,
            @Nullable Collection<? extends GrantedAuthority> authorities,
            DeviceAuthenticationDetails deviceAuthenticationDetails,
            boolean authenticated
    ) {
        super(authorities);
        setDetails(deviceAuthenticationDetails);
        this.principal = principal;
        setAuthenticated(authenticated);
    }

    public DeviceAuthenticationDetails getDetails(){
        return (DeviceAuthenticationDetails) super.getDetails();
    }

    public void setPrincipal(Object jwtUserDetails){
        this.principal = jwtUserDetails;
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }

    @Override
    public @Nullable Object getPrincipal() {
        return principal;
    }

}