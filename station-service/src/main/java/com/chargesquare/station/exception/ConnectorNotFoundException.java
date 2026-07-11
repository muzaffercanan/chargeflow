package com.chargesquare.station.exception;

import org.springframework.http.HttpStatus;

public class ConnectorNotFoundException extends ApiException {

    public ConnectorNotFoundException(Long connectorId) {
        super("CONNECTOR_NOT_FOUND", "Connector %d was not found".formatted(connectorId), HttpStatus.NOT_FOUND);
    }
}
