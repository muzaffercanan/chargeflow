package com.chargesquare.session.client;

import java.math.BigDecimal;

public record StationTariff(Long tariffId, BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
}

