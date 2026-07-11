package com.chargesquare.session.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.chargesquare.session.domain.TariffSnapshot;
import org.springframework.stereotype.Component;

@Component
public class CostCalculator {

    public BigDecimal calculate(BigDecimal energyKwh, TariffSnapshot tariff) {
        return energyKwh.multiply(tariff.getPricePerKwh())
                .add(tariff.getStartFee())
                .setScale(2, RoundingMode.HALF_UP);
    }
}

