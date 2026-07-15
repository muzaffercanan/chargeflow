package com.chargesquare.session.api;

import java.math.BigDecimal;
import java.time.Instant;

import com.chargesquare.session.exception.SessionNotActiveException;
import com.chargesquare.session.service.ChargingSessionService;
import com.chargesquare.session.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ChargingSessionService chargingSessionService;

    @Test
    void startsASessionWithCreatedStatus() throws Exception {
        given(chargingSessionService.start(any(), any())).willReturn(activeSession());

        mockMvc.perform(post("/sessions")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/sessions/1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tariffSnapshot.pricePerKwh").value(8.5));
    }

    @Test
    void rejectsNegativeEnergy() throws Exception {
        mockMvc.perform(post("/sessions/1/stop")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyKwh\":-0.1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsEnergyWithMoreThanSixFractionalDigits() throws Exception {
        mockMvc.perform(post("/sessions/1/stop")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyKwh\":1.1234567}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void mapsRepeatedStopToConflict() throws Exception {
        given(chargingSessionService.stop(any(), any(), any())).willThrow(new SessionNotActiveException(1L));

        mockMvc.perform(post("/sessions/1/stop")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyKwh\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SESSION_NOT_ACTIVE"));
    }

    @Test
    void rejectsViewerStartWithJsonForbiddenResponse() throws Exception {
        mockMvc.perform(post("/sessions")
                        .with(viewer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsViewerStopWithJsonForbiddenResponse() throws Exception {
        mockMvc.perform(post("/sessions/1/stop")
                        .with(viewer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyKwh\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsAnonymousSessionReads() throws Exception {
        mockMvc.perform(get("/sessions/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    private SessionResponse activeSession() {
        return new SessionResponse(
                1L, 7L, 10L, "ACTIVE", Instant.parse("2026-07-11T12:00:00Z"),
                null, null, null,
                new TariffSnapshotResponse(new BigDecimal("8.50"), new BigDecimal("2.00"), "TRY"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().jwt(token -> token.subject("admin").claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor viewer() {
        return jwt().jwt(token -> token.subject("viewer").claim("role", "VIEWER"))
                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }
}
