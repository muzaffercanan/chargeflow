package com.chargesquare.session.api;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Meter-reported energy used to stop, bill, and settle a session.")
public record StopSessionRequest(
        @Schema(example = "12.5", minimum = "0", maximum = "9999999999999.999999")
        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        @Digits(integer = 13, fraction = 6)
        BigDecimal energyKwh) {
}
