package com.hoquangnam45.cdc.image.app.auth.model;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public record RefreshTokenSession(String refreshToken) implements Authentication {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public Object getCredentials() {
        return refreshToken;
    }

    @Override
    public Object getDetails() {
        return refreshToken;
    }

    @Override
    public Object getPrincipal() {
        return refreshToken;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
        return "";
    }
}
