package com.chargesquare.station.service;

import com.chargesquare.station.api.ConnectorStatusResponse;
import com.chargesquare.station.domain.ConnectorStatus;
import com.chargesquare.station.exception.ConnectorNotFoundException;
import com.chargesquare.station.exception.ConnectorOccupiedException;
import com.chargesquare.station.repository.ConnectorRepository;
import com.chargesquare.station.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConnectorServiceTest {

    @Mock
    private ConnectorRepository connectorRepository;

    @Mock
    private StationRepository stationRepository;

    private ConnectorService connectorService;

    @BeforeEach
    void setUp() {
        connectorService = new ConnectorService(connectorRepository, stationRepository);
    }

    @Test
    void occupyUsesAnAtomicAvailableToOccupiedTransition() {
        given(connectorRepository.transitionStatus(10L, ConnectorStatus.AVAILABLE, ConnectorStatus.OCCUPIED))
                .willReturn(1);

        ConnectorStatusResponse response = connectorService.occupy(10L);

        assertThat(response.status()).isEqualTo(ConnectorStatus.OCCUPIED);
        verify(connectorRepository).transitionStatus(10L, ConnectorStatus.AVAILABLE, ConnectorStatus.OCCUPIED);
    }

    @Test
    void occupyReturnsConflictWhenTheConnectorIsAlreadyOccupied() {
        given(connectorRepository.transitionStatus(10L, ConnectorStatus.AVAILABLE, ConnectorStatus.OCCUPIED))
                .willReturn(0);
        given(connectorRepository.existsById(10L)).willReturn(true);

        assertThatThrownBy(() -> connectorService.occupy(10L))
                .isInstanceOf(ConnectorOccupiedException.class)
                .hasMessage("Connector 10 is already OCCUPIED");
    }

    @Test
    void occupyReturnsNotFoundWhenTheConnectorDoesNotExist() {
        given(connectorRepository.transitionStatus(99L, ConnectorStatus.AVAILABLE, ConnectorStatus.OCCUPIED))
                .willReturn(0);
        given(connectorRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> connectorService.occupy(99L))
                .isInstanceOf(ConnectorNotFoundException.class)
                .hasMessage("Connector 99 was not found");
    }

    @Test
    void releaseSetsTheConnectorToAvailable() {
        given(connectorRepository.updateStatus(10L, ConnectorStatus.AVAILABLE)).willReturn(1);

        ConnectorStatusResponse response = connectorService.release(10L);

        assertThat(response.status()).isEqualTo(ConnectorStatus.AVAILABLE);
        verify(connectorRepository).updateStatus(10L, ConnectorStatus.AVAILABLE);
    }
}
