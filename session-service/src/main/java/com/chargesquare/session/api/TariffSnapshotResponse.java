package com.chargesquare.session.api;

import java.math.BigDecimal;

public record TariffSnapshotResponse(BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
}

