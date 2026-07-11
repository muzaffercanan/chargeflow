package com.chargesquare.session.exception;

import org.springframework.http.HttpStatus;

public class ConnectorNotFoundException extends ApiException {
    public ConnectorNotFoundException(Long connectorId) {
        super(HttpStatus.NOT_FOUND, "CONNECTOR_NOT_FOUND", "Connector " + connectorId + " was not found");
    }
}

