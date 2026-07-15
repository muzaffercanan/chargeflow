package com.chargesquare.session.client;

import com.chargesquare.session.exception.ConnectorNotFoundException;
import com.chargesquare.session.exception.ConnectorOccupiedException;
import com.chargesquare.session.exception.StationServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestStationClientTest {

    private MockRestServiceServer server;
    private RestStationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestStationClient(builder.baseUrl("http://station.test").build(), () -> "service-token");
    }

    @Test
    void getsConnectorAndTariffOverHttp() {
        server.expect(requestTo("http://station.test/connectors/10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer admin-token"))
                .andRespond(withSuccess("""
                        {"connectorId":10,"status":"AVAILABLE","tariff":{
                          "tariffId":5,"pricePerKwh":8.50,"startFee":2.00,"currency":"TRY"}}
                        """, MediaType.APPLICATION_JSON));

        StationConnector connector = client.getConnector(10L, "admin-token");

        assertThat(connector.status()).isEqualTo("AVAILABLE");
        assertThat(connector.tariff().pricePerKwh()).isEqualByComparingTo("8.50");
        server.verify();
    }

    @Test
    void occupiesConnectorOverHttp() {
        server.expect(requestTo("http://station.test/connectors/10/occupy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("{\"connectorId\":10,\"status\":\"OCCUPIED\"}", MediaType.APPLICATION_JSON));

        client.occupy(10L);

        server.verify();
    }

    @Test
    void mapsUnknownConnector() {
        server.expect(requestTo("http://station.test/connectors/99"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getConnector(99L, "admin-token"))
                .isInstanceOf(ConnectorNotFoundException.class);
    }

    @Test
    void mapsConflictWhileOccupying() {
        server.expect(requestTo("http://station.test/connectors/10/occupy"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(
                        org.springframework.http.HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.occupy(10L))
                .isInstanceOf(ConnectorOccupiedException.class);
    }

    @Test
    void mapsStationServerFailureToUnavailable() {
        server.expect(requestTo("http://station.test/connectors/10/release"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.release(10L))
                .isInstanceOf(StationServiceUnavailableException.class)
                .hasMessage("Station Service is unavailable");
    }
}
