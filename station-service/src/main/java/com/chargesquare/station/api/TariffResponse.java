package com.chargesquare.station.api;

import java.math.BigDecimal;

public record TariffResponse(Long tariffId, BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
}
