package com.chargesquare.session.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Starts charging for a domain user on an available connector.")
public record StartSessionRequest(
        @Schema(example = "7") @NotNull @Positive Long userId,
        @Schema(example = "10") @NotNull @Positive Long connectorId) {
}
