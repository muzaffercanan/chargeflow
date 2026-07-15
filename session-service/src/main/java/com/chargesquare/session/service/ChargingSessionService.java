package com.chargesquare.session.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.chargesquare.session.api.SessionResponse;
import com.chargesquare.session.api.StartSessionRequest;
import com.chargesquare.session.api.StopSessionRequest;
import com.chargesquare.session.api.StopSessionResponse;
import com.chargesquare.session.client.StationClient;
import com.chargesquare.session.client.StationConnector;
import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.SessionStatus;
import com.chargesquare.session.domain.TariffSnapshot;
import com.chargesquare.session.domain.User;
import com.chargesquare.session.domain.Wallet;
import com.chargesquare.session.exception.ConnectorOccupiedException;
import com.chargesquare.session.exception.SessionNotActiveException;
import com.chargesquare.session.exception.SessionNotFoundException;
import com.chargesquare.session.exception.UserNotFoundException;
import com.chargesquare.session.repository.ChargingSessionRepository;
import com.chargesquare.session.repository.UserRepository;
import com.chargesquare.session.repository.WalletRepository;
import com.chargesquare.session.security.RequestActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargingSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChargingSessionService.class);

    private final ChargingSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final StationClient stationClient;
    private final CostCalculator costCalculator;
    private final Clock clock;

    @Autowired
    public ChargingSessionService(
            ChargingSessionRepository sessionRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            StationClient stationClient,
            CostCalculator costCalculator) {
        this(sessionRepository, userRepository, walletRepository, stationClient, costCalculator, Clock.systemUTC());
    }

    ChargingSessionService(
            ChargingSessionRepository sessionRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            StationClient stationClient,
            CostCalculator costCalculator,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.stationClient = stationClient;
        this.costCalculator = costCalculator;
        this.clock = clock;
    }

    @Transactional
    public SessionResponse start(StartSessionRequest request, RequestActor actor) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        StationConnector connector = stationClient.getConnector(request.connectorId(), actor.accessToken());
        if (!"AVAILABLE".equals(connector.status())) {
            throw new ConnectorOccupiedException(request.connectorId());
        }

        stationClient.occupy(request.connectorId());
        TariffSnapshot tariff = new TariffSnapshot(
                connector.tariff().pricePerKwh(),
                connector.tariff().startFee(),
                connector.tariff().currency());
        ChargingSession session = sessionRepository.save(new ChargingSession(
                user, request.connectorId(), Instant.now(clock), tariff));
        LOGGER.info(
                "event=session_started actor={} role={} sessionId={} userId={} connectorId={}",
                actor.subject(), actor.role(), session.getId(), request.userId(), request.connectorId());
        return SessionResponse.from(session);
    }

    @Transactional
    public StopSessionResponse stop(Long sessionId, StopSessionRequest request, RequestActor actor) {
        ChargingSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotActiveException(sessionId);
        }

        BigDecimal cost = costCalculator.calculate(request.energyKwh(), session.getTariffSnapshot());
        Wallet wallet = walletRepository.findByUserIdForUpdate(session.getUserId())
                .orElseThrow(() -> new UserNotFoundException(session.getUserId()));
        BigDecimal walletBalanceAfter = wallet.debit(cost);
        session.complete(request.energyKwh(), cost, Instant.now(clock));

        stationClient.release(session.getConnectorId());
        LOGGER.info("event=connector_released connectorId={} sessionId={}", session.getConnectorId(), sessionId);
        LOGGER.info("event=cost_charged sessionId={} cost={} currency={}",
                sessionId, cost, session.getTariffSnapshot().getCurrency());
        LOGGER.info("event=wallet_debited sessionId={} userId={} cost={} currency={} balanceAfter={}",
                sessionId, session.getUserId(), cost, session.getTariffSnapshot().getCurrency(), walletBalanceAfter);
        LOGGER.info(
                "event=session_stopped actor={} role={} sessionId={} userId={} connectorId={} "
                        + "energyKwh={} cost={} currency={}",
                actor.subject(), actor.role(), sessionId, session.getUserId(), session.getConnectorId(),
                request.energyKwh(), cost, session.getTariffSnapshot().getCurrency());
        return StopSessionResponse.from(session, walletBalanceAfter);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .map(SessionResponse::from)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return sessionRepository.findAllByUser_IdOrderByStartedAtDesc(userId).stream()
                .map(SessionResponse::from)
                .toList();
    }
}
