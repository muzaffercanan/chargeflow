package com.chargesquare.station.api;

import com.chargesquare.station.domain.ConnectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of an internal connector status transition.")
public record ConnectorStatusResponse(Long connectorId, ConnectorStatus status) {
}
