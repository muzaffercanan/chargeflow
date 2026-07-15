package com.chargesquare.session.api;

import java.math.BigDecimal;
import java.time.Instant;

import com.chargesquare.session.domain.ChargingSession;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Completed-session receipt including the post-debit wallet balance.")
public record StopSessionResponse(
        Long sessionId,
        Long userId,
        Long connectorId,
        String status,
        Instant startedAt,
        Instant endedAt,
        BigDecimal energyKwh,
        BigDecimal cost,
        String currency,
        BigDecimal walletBalanceAfter) {

    public static StopSessionResponse from(ChargingSession session, BigDecimal walletBalanceAfter) {
        return new StopSessionResponse(
                session.getId(),
                session.getUserId(),
                session.getConnectorId(),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getEnergyKwh(),
                session.getCost(),
                session.getTariffSnapshot().getCurrency(),
                walletBalanceAfter);
    }
}
