package com.chargesquare.station.api;

import java.util.List;

import com.chargesquare.station.service.ConnectorService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ConnectorController {

    private final ConnectorService connectorService;

    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @GetMapping("/connectors/{id}")
    public ConnectorResponse getConnector(@PathVariable @Positive Long id) {
        return connectorService.getConnector(id);
    }

    @GetMapping("/stations/{id}/connectors")
    public List<ConnectorResponse> getStationConnectors(@PathVariable @Positive Long id) {
        return connectorService.getStationConnectors(id);
    }

    @PostMapping("/connectors/{id}/occupy")
    public ConnectorStatusResponse occupy(@PathVariable @Positive Long id) {
        return connectorService.occupy(id);
    }

    @PostMapping("/connectors/{id}/release")
    public ConnectorStatusResponse release(@PathVariable @Positive Long id) {
        return connectorService.release(id);
    }
}
