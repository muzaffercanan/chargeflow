package com.chargesquare.session.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short-lived human bearer token and display metadata.")
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        AuthRole role) {
}
