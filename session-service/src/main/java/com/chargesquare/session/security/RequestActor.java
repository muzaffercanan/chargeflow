package com.chargesquare.session.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record RequestActor(String subject, String role, String accessToken) {

    public static RequestActor from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalArgumentException("A JWT-authenticated actor is required");
        }
        return new RequestActor(
                jwtAuthentication.getName(),
                jwtAuthentication.getToken().getClaimAsString("role"),
                jwtAuthentication.getToken().getTokenValue());
    }
}
