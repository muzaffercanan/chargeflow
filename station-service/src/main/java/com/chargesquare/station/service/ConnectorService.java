package com.chargesquare.station.service;

import java.util.List;

import com.chargesquare.station.api.ConnectorResponse;
import com.chargesquare.station.api.ConnectorStatusResponse;
import com.chargesquare.station.api.TariffResponse;
import com.chargesquare.station.domain.Connector;
import com.chargesquare.station.domain.ConnectorStatus;
import com.chargesquare.station.exception.ConnectorNotFoundException;
import com.chargesquare.station.exception.ConnectorOccupiedException;
import com.chargesquare.station.exception.StationNotFoundException;
import com.chargesquare.station.repository.ConnectorRepository;
import com.chargesquare.station.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorService.class);

    private final ConnectorRepository connectorRepository;
    private final StationRepository stationRepository;

    public ConnectorService(ConnectorRepository connectorRepository, StationRepository stationRepository) {
        this.connectorRepository = connectorRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public ConnectorResponse getConnector(Long connectorId) {
        return toResponse(findConnector(connectorId));
    }

    @Transactional(readOnly = true)
    public List<ConnectorResponse> getStationConnectors(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new StationNotFoundException(stationId);
        }

        return connectorRepository.findDetailsByStationId(stationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConnectorStatusResponse occupy(Long connectorId) {
        int updatedRows = connectorRepository.transitionStatus(
                connectorId,
                ConnectorStatus.AVAILABLE,
                ConnectorStatus.OCCUPIED);

        if (updatedRows == 1) {
            LOGGER.info("event=connector_occupied connectorId={}", connectorId);
            return new ConnectorStatusResponse(connectorId, ConnectorStatus.OCCUPIED);
        }

        if (!connectorRepository.existsById(connectorId)) {
            throw new ConnectorNotFoundException(connectorId);
        }

        throw new ConnectorOccupiedException(connectorId);
    }

    @Transactional
    public ConnectorStatusResponse release(Long connectorId) {
        int updatedRows = connectorRepository.updateStatus(connectorId, ConnectorStatus.AVAILABLE);
        if (updatedRows == 0) {
            throw new ConnectorNotFoundException(connectorId);
        }

        LOGGER.info("event=connector_released connectorId={}", connectorId);
        return new ConnectorStatusResponse(connectorId, ConnectorStatus.AVAILABLE);
    }

    private Connector findConnector(Long connectorId) {
        return connectorRepository.findDetailsById(connectorId)
                .orElseThrow(() -> new ConnectorNotFoundException(connectorId));
    }

    private ConnectorResponse toResponse(Connector connector) {
        return new ConnectorResponse(
                connector.getId(),
                connector.getStation().getId(),
                connector.getType(),
                connector.getPowerKw(),
                connector.getStatus(),
                new TariffResponse(
                        connector.getTariff().getId(),
                        connector.getTariff().getPricePerKwh(),
                        connector.getTariff().getStartFee(),
                        connector.getTariff().getCurrency()));
    }
}
