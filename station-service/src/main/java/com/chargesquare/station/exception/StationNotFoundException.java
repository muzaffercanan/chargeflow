package com.chargesquare.station.exception;

import org.springframework.http.HttpStatus;

public class StationNotFoundException extends ApiException {

    public StationNotFoundException(Long stationId) {
        super("STATION_NOT_FOUND", "Station %d was not found".formatted(stationId), HttpStatus.NOT_FOUND);
    }
}
