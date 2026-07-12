package com.chargesquare.station.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tariffs")
public class Tariff {

    @Id
    private Long id;

    @Column(name = "price_per_kwh", nullable = false, precision = 19, scale = 2)
    private BigDecimal pricePerKwh;

    @Column(name = "start_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal startFee;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Tariff() {
    }

    public Tariff(Long id, BigDecimal pricePerKwh, BigDecimal startFee, String currency) {
        this.id = id;
        this.pricePerKwh = pricePerKwh;
        this.startFee = startFee;
        this.currency = currency;
    }

    public Long getId() {
        return id;
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
