package com.chargesquare.session.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TariffSnapshot {

    @Column(name = "tariff_price_per_kwh", nullable = false, precision = 19, scale = 6)
    private BigDecimal pricePerKwh;

    @Column(name = "tariff_start_fee", nullable = false, precision = 19, scale = 6)
    private BigDecimal startFee;

    @Column(name = "tariff_currency", nullable = false, length = 3)
    private String currency;

    protected TariffSnapshot() {
    }

    public TariffSnapshot(BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
        this.pricePerKwh = pricePerKwh;
        this.startFee = startFee;
        this.currency = currency;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public BigDecimal getStartFee() {
        return startFee;
    }

    public String getCurrency() {
        return currency;
    }
}

