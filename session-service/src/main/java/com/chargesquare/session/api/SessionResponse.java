package com.chargesquare.session.api;

import java.math.BigDecimal;
import java.time.Instant;

import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.TariffSnapshot;

public record SessionResponse(
        Long sessionId,
        Long userId,
        Long connectorId,
        String status,
        Instant startedAt,
        Instant endedAt,
        BigDecimal energyKwh,
        BigDecimal cost,
        TariffSnapshotResponse tariffSnapshot) {

    public static SessionResponse from(ChargingSession session) {
        TariffSnapshot tariff = session.getTariffSnapshot();
        return new SessionResponse(
                session.getId(),
                session.getUserId(),
                session.getConnectorId(),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getEnergyKwh(),
                session.getCost(),
                new TariffSnapshotResponse(tariff.getPricePerKwh(), tariff.getStartFee(), tariff.getCurrency()));
    }
}

