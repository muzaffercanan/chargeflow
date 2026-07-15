package com.chargesquare.session.client;

import java.net.SocketTimeoutException;
import java.time.Duration;

import com.chargesquare.session.auth.ServiceTokenProvider;
import com.chargesquare.session.exception.ConnectorNotFoundException;
import com.chargesquare.session.exception.ConnectorOccupiedException;
import com.chargesquare.session.exception.StationServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestStationClient implements StationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestStationClient.class);

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;

    @Autowired
    public RestStationClient(
            RestClient.Builder builder,
            @Value("${station-service.base-url}") String baseUrl,
            @Value("${station-service.connect-timeout}") Duration connectTimeout,
            @Value("${station-service.read-timeout}") Duration readTimeout,
            ServiceTokenProvider serviceTokenProvider) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.requestFactory(requestFactory).baseUrl(baseUrl).build();
        this.serviceTokenProvider = serviceTokenProvider;
    }

    RestStationClient(RestClient restClient, ServiceTokenProvider serviceTokenProvider) {
        this.restClient = restClient;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    @Override
    public StationConnector getConnector(Long connectorId, String humanAccessToken) {
        try {
            return restClient.get()
                    .uri("/connectors/{id}", connectorId)
                    .headers(headers -> headers.setBearerAuth(humanAccessToken))
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> { throw new ConnectorNotFoundException(connectorId); })
                    .body(StationConnector.class);
        } catch (ResourceAccessException exception) {
            logDependencyFailure(exception, "get_connector", connectorId);
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
                    .headers(headers -> headers.setBearerAuth(serviceTokenProvider.issueServiceToken()))
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> { throw new ConnectorNotFoundException(connectorId); })
                    .onStatus(status -> status.value() == 409,
                            (request, response) -> { throw new ConnectorOccupiedException(connectorId); })
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            logDependencyFailure(exception, action, connectorId);
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

    private void logDependencyFailure(ResourceAccessException exception, String operation, Long connectorId) {
        if (hasCause(exception, SocketTimeoutException.class)) {
            LOGGER.warn("event=station_dependency_timeout operation={} connectorId={}", operation, connectorId);
            return;
        }
        LOGGER.warn("event=station_dependency_unavailable operation={} connectorId={}", operation, connectorId);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
