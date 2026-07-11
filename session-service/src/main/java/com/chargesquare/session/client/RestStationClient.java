package com.chargesquare.session.client;

import com.chargesquare.session.exception.ConnectorNotFoundException;
import com.chargesquare.session.exception.ConnectorOccupiedException;
import com.chargesquare.session.exception.StationServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestStationClient implements StationClient {

    private final RestClient restClient;

    public RestStationClient(RestClient.Builder builder, @Value("${station-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public StationConnector getConnector(Long connectorId) {
        try {
            return restClient.get()
                    .uri("/connectors/{id}", connectorId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> { throw new ConnectorNotFoundException(connectorId); })
                    .body(StationConnector.class);
        } catch (ResourceAccessException exception) {
            throw new StationServiceUnavailableException();
        } catch (RestClientResponseException exception) {
            throw mapUnexpectedResponse(exception.getStatusCode(), connectorId);
        } catch (RestClientException exception) {
            throw new StationServiceUnavailableException();
        }
    }

    @Override
    public void occupy(Long connectorId) {
        postTransition(connectorId, "occupy");
    }

    @Override
    public void release(Long connectorId) {
        postTransition(connectorId, "release");
    }

    private void postTransition(Long connectorId, String action) {
        try {
            restClient.post()
                    .uri("/connectors/{id}/" + action, connectorId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> { throw new ConnectorNotFoundException(connectorId); })
                    .onStatus(status -> status.value() == 409,
                            (request, response) -> { throw new ConnectorOccupiedException(connectorId); })
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new StationServiceUnavailableException();
        } catch (RestClientResponseException exception) {
            throw mapUnexpectedResponse(exception.getStatusCode(), connectorId);
        } catch (RestClientException exception) {
            throw new StationServiceUnavailableException();
        }
    }

    private RuntimeException mapUnexpectedResponse(HttpStatusCode status, Long connectorId) {
        if (status.value() == 404) {
            return new ConnectorNotFoundException(connectorId);
        }
        if (status.value() == 409) {
            return new ConnectorOccupiedException(connectorId);
        }
        return new StationServiceUnavailableException();
    }
}
