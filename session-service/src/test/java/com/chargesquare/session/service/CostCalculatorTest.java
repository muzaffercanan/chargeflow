package com.chargesquare.session.service;

import java.math.BigDecimal;

import com.chargesquare.session.domain.TariffSnapshot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CostCalculatorTest {

    private final CostCalculator calculator = new CostCalculator();

    @Test
    void calculatesTheWorkedBillingExample() {
        TariffSnapshot tariff = new TariffSnapshot(
                new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY");

        BigDecimal cost = calculator.calculate(new BigDecimal("12.5"), tariff);

        assertThat(cost).isEqualByComparingTo("108.25");
    }

    @Test
    void allowsZeroEnergyAndStillChargesTheStartFee() {
        TariffSnapshot tariff = new TariffSnapshot(
                new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY");

        assertThat(calculator.calculate(BigDecimal.ZERO, tariff)).isEqualByComparingTo("2.00");
    }
}

