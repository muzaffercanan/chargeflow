package com.chargesquare.session.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Demo human login credentials.")
public record LoginRequest(
        @Schema(example = "admin") @NotBlank @Size(max = 100) String username,
        @Schema(example = "admin-demo", format = "password") @NotBlank @Size(max = 200) String password) {
}
