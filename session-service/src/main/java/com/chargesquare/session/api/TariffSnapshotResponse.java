package com.chargesquare.session.api;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Immutable tariff values captured when the session starts.")
public record TariffSnapshotResponse(BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
}
