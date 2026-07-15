package com.chargesquare.station.api;

import java.math.BigDecimal;
import java.util.List;

import com.chargesquare.station.domain.ConnectorStatus;
import com.chargesquare.station.exception.ConnectorNotFoundException;
import com.chargesquare.station.exception.ConnectorOccupiedException;
import com.chargesquare.station.service.ConnectorService;
import com.chargesquare.station.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectorController.class)
@Import(SecurityConfig.class)
class ConnectorControllerTest {

    private static final TariffResponse TARIFF = new TariffResponse(
            5L,
            new BigDecimal("8.50"),
            new BigDecimal("2.00"),
            "TRY");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectorService connectorService;

    @Test
    void getsConnectorById() throws Exception {
        given(connectorService.getConnector(10L)).willReturn(connector(10L, "CCS2-DC", 60));

        mockMvc.perform(get("/connectors/10").with(viewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectorId").value(10))
                .andExpect(jsonPath("$.stationId").value(1))
                .andExpect(jsonPath("$.type").value("CCS2-DC"))
                .andExpect(jsonPath("$.powerKw").value(60))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.tariff.tariffId").value(5))
                .andExpect(jsonPath("$.tariff.pricePerKwh").value(8.5))
                .andExpect(jsonPath("$.tariff.startFee").value(2.0))
                .andExpect(jsonPath("$.tariff.currency").value("TRY"));
    }

    @Test
    void listsStationConnectors() throws Exception {
        given(connectorService.getStationConnectors(1L)).willReturn(List.of(
                connector(10L, "CCS2-DC", 60),
                connector(11L, "Type2-AC", 22)));

        mockMvc.perform(get("/stations/1/connectors").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].connectorId").value(10))
                .andExpect(jsonPath("$[1].connectorId").value(11))
                .andExpect(jsonPath("$[1].type").value("Type2-AC"));
    }

    @Test
    void occupiesAnAvailableConnector() throws Exception {
        given(connectorService.occupy(10L)).willReturn(new ConnectorStatusResponse(10L, ConnectorStatus.OCCUPIED));

        mockMvc.perform(post("/connectors/10/occupy").with(service()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectorId").value(10))
                .andExpect(jsonPath("$.status").value("OCCUPIED"));
    }

    @Test
    void releasesAConnector() throws Exception {
        given(connectorService.release(10L)).willReturn(new ConnectorStatusResponse(10L, ConnectorStatus.AVAILABLE));

        mockMvc.perform(post("/connectors/10/release").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectorId").value(10))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void rejectsOccupyingAnOccupiedConnector() throws Exception {
        given(connectorService.occupy(10L)).willThrow(new ConnectorOccupiedException(10L));

        mockMvc.perform(post("/connectors/10/occupy").with(service()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONNECTOR_OCCUPIED"))
                .andExpect(jsonPath("$.message").value("Connector 10 is already OCCUPIED"));
    }

    @Test
    void returnsNotFoundForAnUnknownConnector() throws Exception {
        given(connectorService.getConnector(99L)).willThrow(new ConnectorNotFoundException(99L));

        mockMvc.perform(get("/connectors/99").with(viewer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CONNECTOR_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Connector 99 was not found"));
    }

    @Test
    void rejectsAnonymousReadsWithJsonUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/connectors/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsViewerInternalWrites() throws Exception {
        mockMvc.perform(post("/connectors/10/occupy").with(viewer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsAnonymousInternalWrites() throws Exception {
        mockMvc.perform(post("/connectors/10/release"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    private ConnectorResponse connector(Long id, String type, int powerKw) {
        return new ConnectorResponse(id, 1L, type, powerKw, ConnectorStatus.AVAILABLE, TARIFF);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor viewer() {
        return jwt().jwt(token -> token.subject("viewer").claim("role", "VIEWER"))
                .authorities(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().jwt(token -> token.subject("admin").claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor service() {
        return jwt().jwt(token -> token.subject("session-service").claim("role", "SERVICE"))
                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"));
    }
}
