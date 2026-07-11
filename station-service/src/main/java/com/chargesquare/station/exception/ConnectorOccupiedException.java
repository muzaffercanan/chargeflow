package com.chargesquare.station.exception;

import org.springframework.http.HttpStatus;

public class ConnectorOccupiedException extends ApiException {

    public ConnectorOccupiedException(Long connectorId) {
        super("CONNECTOR_OCCUPIED", "Connector %d is already OCCUPIED".formatted(connectorId), HttpStatus.CONFLICT);
    }
}
