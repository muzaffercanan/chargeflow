package com.chargesquare.station.api;

import com.chargesquare.station.domain.ConnectorStatus;

public record ConnectorStatusResponse(Long connectorId, ConnectorStatus status) {
}
