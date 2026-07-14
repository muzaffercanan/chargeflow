package com.chargesquare.session.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record StopSessionRequest(
        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        @Digits(integer = 13, fraction = 6)
        BigDecimal energyKwh) {
}
