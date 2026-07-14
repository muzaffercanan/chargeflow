package com.chargesquare.session;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.chargesquare.session.domain.ChargingSession;
import com.chargesquare.session.domain.SessionStatus;
import com.chargesquare.session.repository.ChargingSessionRepository;
import com.chargesquare.session.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionDependencyTimeoutIntegrationTest {

    private static final DelayedStationServer STATION_SERVER = DelayedStationServer.start();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChargingSessionRepository sessionRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void stationProperties(DynamicPropertyRegistry registry) {
        registry.add("station-service.base-url", STATION_SERVER::baseUrl);
        registry.add("station-service.connect-timeout", () -> "100ms");
        registry.add("station-service.read-timeout", () -> "150ms");
    }

    @BeforeEach
    void resetState() {
        sessionRepository.deleteAll();
        jdbcTemplate.update("update session.wallets set balance = 500.00 where user_id = 7");
        STATION_SERVER.reset();
    }

    @AfterAll
    static void stopServer() {
        STATION_SERVER.stop();
    }

    @Test
    void startTimesOutWithoutPersistingOrAdvancingLocalState() throws Exception {
        STATION_SERVER.blockConnectorRead();

        long startedAt = System.nanoTime();
        mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("STATION_SERVICE_UNAVAILABLE"));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(elapsed).isBetween(Duration.ofMillis(100), Duration.ofSeconds(2));
        assertThat(sessionRepository.count()).isZero();
        assertThat(STATION_SERVER.connectorStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void releaseTimeoutRollsBackSessionAndWalletAndCannotCommitLater() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = objectMapper.readTree(startResult.getResponse().getContentAsString())
                .path("sessionId").asLong();
        STATION_SERVER.blockRelease();

        long startedAt = System.nanoTime();
        mockMvc.perform(post("/sessions/{id}/stop", sessionId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"energyKwh\":12.5}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("STATION_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Station Service is unavailable"));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(elapsed).isBetween(Duration.ofMillis(100), Duration.ofSeconds(2));
        assertActiveAndUncharged(sessionId);

        STATION_SERVER.resumeResponses();
        Thread.sleep(900);

        assertActiveAndUncharged(sessionId);
        assertThat(STATION_SERVER.connectorStatus()).isEqualTo("OCCUPIED");
    }

    private void assertActiveAndUncharged(long sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getEndedAt()).isNull();
        assertThat(session.getEnergyKwh()).isNull();
        assertThat(session.getCost()).isNull();
        assertThat(walletRepository.findById(7L).orElseThrow().getBalance()).isEqualByComparingTo("500.00");
    }

    private static final class DelayedStationServer {

        private enum Behavior { NORMAL, BLOCK_CONNECTOR_READ, BLOCK_RELEASE }

        private final HttpServer server;
        private final AtomicReference<Behavior> behavior = new AtomicReference<>(Behavior.NORMAL);
        private final AtomicReference<String> connectorStatus = new AtomicReference<>("AVAILABLE");

        private DelayedStationServer(HttpServer server) {
            this.server = server;
        }

        static DelayedStationServer start() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                DelayedStationServer stationServer = new DelayedStationServer(server);
                server.createContext("/", stationServer::handle);
                server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                    Thread thread = new Thread(runnable, "delayed-station-test-server");
                    thread.setDaemon(true);
                    return thread;
                }));
                server.start();
                return stationServer;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not start delayed Station test server", exception);
            }
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void reset() {
            behavior.set(Behavior.NORMAL);
            connectorStatus.set("AVAILABLE");
        }

        void blockConnectorRead() {
            behavior.set(Behavior.BLOCK_CONNECTOR_READ);
        }

        void blockRelease() {
            behavior.set(Behavior.BLOCK_RELEASE);
        }

        void resumeResponses() {
            behavior.set(Behavior.NORMAL);
        }

        String connectorStatus() {
            return connectorStatus.get();
        }

        void stop() {
            server.stop(0);
        }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(exchange.getRequestMethod()) && "/connectors/10".equals(path)) {
                if (behavior.get() == Behavior.BLOCK_CONNECTOR_READ) {
                    delayWithoutResponse(exchange);
                    return;
                }
                respond(exchange, 200, """
                        {"connectorId":10,"status":"%s","tariff":{
                        "tariffId":5,"pricePerKwh":8.50,"startFee":2.00,"currency":"TRY"}}
                        """.formatted(connectorStatus.get()));
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && "/connectors/10/occupy".equals(path)) {
                connectorStatus.set("OCCUPIED");
                respond(exchange, 200, "{\"connectorId\":10,\"status\":\"OCCUPIED\"}");
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && "/connectors/10/release".equals(path)) {
                if (behavior.get() == Behavior.BLOCK_RELEASE) {
                    delayWithoutResponse(exchange);
                    return;
                }
                connectorStatus.set("AVAILABLE");
                respond(exchange, 200, "{\"connectorId\":10,\"status\":\"AVAILABLE\"}");
                return;
            }
            respond(exchange, 404, "{}");
        }

        private void delayWithoutResponse(HttpExchange exchange) {
            try {
                Thread.sleep(750);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        }

        private void respond(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }
}
