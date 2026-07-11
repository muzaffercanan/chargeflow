package com.chargesquare.session.exception;

import org.springframework.http.HttpStatus;

public class StationServiceUnavailableException extends ApiException {
    public StationServiceUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "STATION_SERVICE_UNAVAILABLE", "Station Service is unavailable");
    }
}

