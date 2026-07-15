package com.chargesquare.station.api;

import com.chargesquare.station.domain.ConnectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Connector details and its current tariff.")
public record ConnectorResponse(
        Long connectorId,
        Long stationId,
        String type,
        Integer powerKw,
        ConnectorStatus status,
        TariffResponse tariff) {
}
