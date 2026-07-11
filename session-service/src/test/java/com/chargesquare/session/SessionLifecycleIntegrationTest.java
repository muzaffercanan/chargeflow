package com.chargesquare.session;

import java.math.BigDecimal;

import com.chargesquare.session.client.StationClient;
import com.chargesquare.session.client.StationConnector;
import com.chargesquare.session.client.StationTariff;
import com.chargesquare.session.domain.SessionStatus;
import com.chargesquare.session.exception.ConnectorNotFoundException;
import com.chargesquare.session.exception.ConnectorOccupiedException;
import com.chargesquare.session.exception.StationServiceUnavailableException;
import com.chargesquare.session.repository.ChargingSessionRepository;
import com.chargesquare.session.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SessionLifecycleIntegrationTest.StationClientTestConfiguration.class)
class SessionLifecycleIntegrationTest {

    private static final BigDecimal SEEDED_WALLET_BALANCE = new BigDecimal("500.00");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChargingSessionRepository sessionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatefulStationClient stationClient;

    @BeforeEach
    void resetState() {
        sessionRepository.deleteAll();
        jdbcTemplate.update("update session.wallets set balance = ? where user_id = 7", SEEDED_WALLET_BALANCE);
        stationClient.reset();
    }

    @Test
    void persistsTheFullStartToStopLifecycleUsingTheTariffCapturedAtStart() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tariffSnapshot.pricePerKwh").value(8.5))
                .andReturn();

        long sessionId = response(startResult).path("sessionId").asLong();
        assertThat(stationClient.status()).isEqualTo("OCCUPIED");

        stationClient.changeTariff(new BigDecimal("99.99"), new BigDecimal("50.00"));

        mockMvc.perform(post("/sessions/{id}/stop", sessionId)
                .contentType(APPLICATION_JSON)
                .content("{\"energyKwh\":12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cost").value(108.25))
                .andExpect(jsonPath("$.walletBalanceAfter").value(391.75));

        var completedSession = sessionRepository.findById(sessionId).orElseThrow();
        var firstEndTime = completedSession.getEndedAt();
        assertThat(completedSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completedSession.getCost()).isEqualByComparingTo("108.25");
        assertThat(completedSession.getTariffSnapshot().getPricePerKwh()).isEqualByComparingTo("8.50");
        assertThat(walletRepository.findById(7L).orElseThrow().getBalance()).isEqualByComparingTo("391.75");
        assertThat(stationClient.status()).isEqualTo("AVAILABLE");

        mockMvc.perform(post("/sessions/{id}/stop", sessionId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"energyKwh\":12.5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_ACTIVE"));

        var sessionAfterRepeatedStop = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(sessionAfterRepeatedStop.getEndedAt()).isEqualTo(firstEndTime);
        assertThat(walletRepository.findById(7L).orElseThrow().getBalance()).isEqualByComparingTo("391.75");
    }

    @Test
    void rejectsUnknownOrOccupiedConnectorsWithoutPersistingSessions() throws Exception {
        stationClient.makeUnknown();

        mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":99}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CONNECTOR_NOT_FOUND"));
        assertThat(sessionRepository.count()).isZero();

        stationClient.reset();
        stationClient.makeOccupied();

        mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONNECTOR_OCCUPIED"));
        assertThat(sessionRepository.count()).isZero();
    }

    @Test
    void returnsValidationErrorsBeforeCreatingOrStoppingSessions() throws Exception {
        mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"connectorId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/sessions/1/stop")
                        .contentType(APPLICATION_JSON)
                        .content("{\"energyKwh\":-0.01}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        assertThat(sessionRepository.count()).isZero();
    }

    @Test
    void returnsAConsistentServiceUnavailableErrorWhenStationServiceCannotBeReached() throws Exception {
        stationClient.makeUnavailable();

        mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("STATION_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Station Service is unavailable"));

        assertThat(sessionRepository.count()).isZero();
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StationClientTestConfiguration {

        @Bean
        @Primary
        StatefulStationClient stationClient() {
            return new StatefulStationClient();
        }
    }

    static class StatefulStationClient implements StationClient {

        private static final long CONNECTOR_ID = 10L;
        private static final long TARIFF_ID = 5L;
        private static final String CURRENCY = "TRY";

        private String status;
        private boolean unknown;
        private boolean unavailable;
        private StationTariff tariff;

        void reset() {
            status = "AVAILABLE";
            unknown = false;
            unavailable = false;
            tariff = new StationTariff(TARIFF_ID, new BigDecimal("8.50"), new BigDecimal("2.00"), CURRENCY);
        }

        void makeUnknown() {
            unknown = true;
        }

        void makeOccupied() {
            status = "OCCUPIED";
        }

        void makeUnavailable() {
            unavailable = true;
        }

        void changeTariff(BigDecimal pricePerKwh, BigDecimal startFee) {
            tariff = new StationTariff(TARIFF_ID, pricePerKwh, startFee, CURRENCY);
        }

        String status() {
            return status;
        }

        @Override
        public StationConnector getConnector(Long connectorId) {
            verifyAvailable();
            verifyKnownConnector(connectorId);
            return new StationConnector(connectorId, status, tariff);
        }

        @Override
        public void occupy(Long connectorId) {
            verifyAvailable();
            verifyKnownConnector(connectorId);
            if (!"AVAILABLE".equals(status)) {
                throw new ConnectorOccupiedException(connectorId);
            }
            status = "OCCUPIED";
        }

        @Override
        public void release(Long connectorId) {
            verifyAvailable();
            verifyKnownConnector(connectorId);
            status = "AVAILABLE";
        }

        private void verifyAvailable() {
            if (unavailable) {
                throw new StationServiceUnavailableException();
            }
        }

        private void verifyKnownConnector(Long connectorId) {
            if (unknown || connectorId == null || connectorId != CONNECTOR_ID) {
                throw new ConnectorNotFoundException(connectorId);
            }
        }
    }
}
