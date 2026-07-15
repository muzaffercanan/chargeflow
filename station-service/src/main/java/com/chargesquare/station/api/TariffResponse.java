package com.chargesquare.station.api;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tariff applied by a connector.")
public record TariffResponse(Long tariffId, BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
}
