package com.chargesquare.session.client;

public interface StationClient {
    StationConnector getConnector(Long connectorId);
    void occupy(Long connectorId);
    void release(Long connectorId);
}

