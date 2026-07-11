package com.chargesquare.session.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record StopSessionRequest(
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal energyKwh) {
}

