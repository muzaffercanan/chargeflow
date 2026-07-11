package com.chargesquare.session.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StartSessionRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long connectorId) {
}

