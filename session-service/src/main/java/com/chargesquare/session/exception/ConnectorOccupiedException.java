package com.chargesquare.session.exception;

import org.springframework.http.HttpStatus;

public class ConnectorOccupiedException extends ApiException {
    public ConnectorOccupiedException(Long connectorId) {
        super(HttpStatus.CONFLICT, "CONNECTOR_OCCUPIED", "Connector " + connectorId + " is not AVAILABLE");
    }
}

