package com.chargesquare.session.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.chargesquare.session.api.StartSessionRequest;
import com.chargesquare.session.api.StopSessionRequest;
import com.chargesquare.session.api.StopSessionResponse;
import com.chargesquare.session.client.StationClient;
import com.chargesquare.session.client.StationConnector;
import com.chargesquare.session.client.StationTariff;
import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.TariffSnapshot;
import com.chargesquare.session.domain.User;
import com.chargesquare.session.domain.Wallet;
import com.chargesquare.session.exception.ConnectorOccupiedException;
import com.chargesquare.session.exception.SessionNotActiveException;
import com.chargesquare.session.exception.StationServiceUnavailableException;
import com.chargesquare.session.repository.ChargingSessionRepository;
import com.chargesquare.session.repository.UserRepository;
import com.chargesquare.session.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargingSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");

    @Mock private ChargingSessionRepository sessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private StationClient stationClient;
    @Mock private User user;
    @Mock private Wallet wallet;

    private ChargingSessionService service;

    @BeforeEach
    void setUp() {
        service = new ChargingSessionService(
                sessionRepository,
                userRepository,
                walletRepository,
                stationClient,
                new CostCalculator(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void startsAnActiveSessionWithTheStationTariffSnapshot() {
        when(user.getId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(stationClient.getConnector(10L)).thenReturn(connector("AVAILABLE"));
        when(sessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.start(new StartSessionRequest(7L, 10L));

        ArgumentCaptor<ChargingSession> captor = ArgumentCaptor.forClass(ChargingSession.class);
        verify(sessionRepository).save(captor.capture());
        verify(stationClient).occupy(10L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.startedAt()).isEqualTo(NOW);
        assertThat(captor.getValue().getTariffSnapshot().getPricePerKwh()).isEqualByComparingTo("8.50");
    }

    @Test
    void doesNotPersistWhenConnectorIsNotAvailable() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(stationClient.getConnector(10L)).thenReturn(connector("OCCUPIED"));

        assertThatThrownBy(() -> service.start(new StartSessionRequest(7L, 10L)))
                .isInstanceOf(ConnectorOccupiedException.class);

        verify(stationClient, never()).occupy(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void doesNotPersistWhenOccupyFails() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(stationClient.getConnector(10L)).thenReturn(connector("AVAILABLE"));
        org.mockito.Mockito.doThrow(new StationServiceUnavailableException()).when(stationClient).occupy(10L);

        assertThatThrownBy(() -> service.start(new StartSessionRequest(7L, 10L)))
                .isInstanceOf(StationServiceUnavailableException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void stopsBillsSettlesAndReleases() {
        when(user.getId()).thenReturn(7L);
        ChargingSession session = activeSession();
        when(sessionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(wallet));
        when(wallet.debit(new BigDecimal("108.25"))).thenReturn(new BigDecimal("391.75"));

        StopSessionResponse receipt = service.stop(1L, new StopSessionRequest(new BigDecimal("12.5")));

        assertThat(receipt.status()).isEqualTo("COMPLETED");
        assertThat(receipt.cost()).isEqualByComparingTo("108.25");
        assertThat(receipt.walletBalanceAfter()).isEqualByComparingTo("391.75");
        assertThat(receipt.endedAt()).isEqualTo(NOW);
        verify(wallet).debit(new BigDecimal("108.25"));
        verify(stationClient).release(10L);
    }

    @Test
    void repeatedStopChangesNothing() {
        ChargingSession session = activeSession();
        session.complete(BigDecimal.ONE, new BigDecimal("10.50"), NOW);
        when(sessionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.stop(1L, new StopSessionRequest(BigDecimal.ONE)))
                .isInstanceOf(SessionNotActiveException.class);

        verify(walletRepository, never()).findByUserIdForUpdate(any());
        verify(stationClient, never()).release(any());
    }

    private ChargingSession activeSession() {
        return new ChargingSession(
                user,
                10L,
                NOW.minusSeconds(3600),
                new TariffSnapshot(new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY"));
    }

    private StationConnector connector(String status) {
        return new StationConnector(
                10L,
                status,
                new StationTariff(5L, new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY"));
    }
}
