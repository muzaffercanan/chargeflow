package com.chargesquare.session.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        AuthRole role) {
}
