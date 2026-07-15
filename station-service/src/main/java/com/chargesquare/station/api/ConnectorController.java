package com.chargesquare.station.api;

import java.util.List;

import com.chargesquare.station.service.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Station and connector operations")
public class ConnectorController {

    private final ConnectorService connectorService;

    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @GetMapping("/connectors/{id}")
    @Operation(
            summary = "Get a connector",
            description = "Requires a VIEWER or ADMIN JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connector and current tariff",
                    content = @Content(schema = @Schema(implementation = ConnectorResponse.class),
                            examples = @ExampleObject(value = """
                                    {"connectorId":10,"stationId":1,"type":"CCS2-DC","powerKw":60,
                                     "status":"AVAILABLE","tariff":{"tariffId":5,"pricePerKwh":8.50,
                                     "startFee":2.00,"currency":"TRY"}}
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid connector id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "SERVICE tokens cannot use human read endpoints",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Connector not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ConnectorResponse getConnector(@PathVariable @Positive Long id) {
        return connectorService.getConnector(id);
    }

    @GetMapping("/stations/{id}/connectors")
    @Operation(
            summary = "List a station's connectors",
            description = "Requires a VIEWER or ADMIN JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connectors with current status and tariff",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectorResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid station id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "SERVICE tokens cannot use human read endpoints",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Station not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<ConnectorResponse> getStationConnectors(@PathVariable @Positive Long id) {
        return connectorService.getStationConnectors(id);
    }

    @PostMapping("/connectors/{id}/occupy")
    @Operation(
            summary = "Occupy a connector",
            description = "Internal transition. Requires a short-lived SERVICE JWT; direct ADMIN access is also allowed.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connector changed to OCCUPIED",
                    content = @Content(schema = @Schema(implementation = ConnectorStatusResponse.class),
                            examples = @ExampleObject(value = "{\"connectorId\":10,\"status\":\"OCCUPIED\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid connector id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "JWT lacks SERVICE or ADMIN role",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Connector not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Connector is already OCCUPIED",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ConnectorStatusResponse occupy(@PathVariable @Positive Long id) {
        return connectorService.occupy(id);
    }

    @PostMapping("/connectors/{id}/release")
    @Operation(
            summary = "Release a connector",
            description = "Internal idempotent transition for an existing connector. Requires SERVICE or ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connector is AVAILABLE",
                    content = @Content(schema = @Schema(implementation = ConnectorStatusResponse.class),
                            examples = @ExampleObject(value = "{\"connectorId\":10,\"status\":\"AVAILABLE\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid connector id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "JWT lacks SERVICE or ADMIN role",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Connector not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ConnectorStatusResponse release(@PathVariable @Positive Long id) {
        return connectorService.release(id);
    }
}
