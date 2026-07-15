package com.chargesquare.session.api;

import java.net.URI;
import java.util.List;

import com.chargesquare.session.security.RequestActor;
import com.chargesquare.session.service.ChargingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Charging sessions")
public class SessionController {

    private final ChargingSessionService chargingSessionService;

    public SessionController(ChargingSessionService chargingSessionService) {
        this.chargingSessionService = chargingSessionService;
    }

    @PostMapping("/sessions")
    @Operation(
            summary = "Start a charging session",
            description = "Requires ADMIN. Reads and occupies the connector through Station Service, then snapshots its tariff.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = StartSessionRequest.class),
                            examples = @ExampleObject(value = "{\"userId\":7,\"connectorId\":10}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "ACTIVE session created",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class),
                            examples = @ExampleObject(value = """
                                    {"sessionId":1,"userId":7,"connectorId":10,"status":"ACTIVE",
                                     "startedAt":"2026-07-15T08:00:00Z","endedAt":null,"energyKwh":null,
                                     "cost":null,"tariffSnapshot":{"pricePerKwh":8.50,"startFee":2.00,"currency":"TRY"}}
                                    """))),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "JWT lacks ADMIN role",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User or connector not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Connector is not AVAILABLE",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Station Service unavailable or timed out",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SessionResponse> start(
            @Valid @RequestBody StartSessionRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        SessionResponse response = chargingSessionService.start(request, RequestActor.from(authentication));
        return ResponseEntity.created(URI.create("/sessions/" + response.sessionId())).body(response);
    }

    @PostMapping("/sessions/{id}/stop")
    @Operation(
            summary = "Stop, bill, and settle a charging session",
            description = "Requires ADMIN. Uses the tariff snapshot, debits the wallet, completes the session, and releases the connector.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = StopSessionRequest.class),
                            examples = @ExampleObject(value = "{\"energyKwh\":12.5}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "COMPLETED receipt; worked billing example is 108.25 TRY",
                    content = @Content(schema = @Schema(implementation = StopSessionResponse.class),
                            examples = @ExampleObject(value = """
                                    {"sessionId":1,"userId":7,"connectorId":10,"status":"COMPLETED",
                                     "startedAt":"2026-07-15T08:00:00Z","endedAt":"2026-07-15T08:45:00Z",
                                     "energyKwh":12.5,"cost":108.25,"currency":"TRY","walletBalanceAfter":391.75}
                                    """))),
            @ApiResponse(responseCode = "400", description = "Energy is missing, negative, or exceeds six fractional digits",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "JWT lacks ADMIN role",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Session is not ACTIVE, including a second stop",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Station Service unavailable or release timed out",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public StopSessionResponse stop(
            @PathVariable @Positive Long id,
            @Valid @RequestBody StopSessionRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        return chargingSessionService.stop(id, request, RequestActor.from(authentication));
    }

    @GetMapping("/sessions/{id}")
    @Operation(
            summary = "Get a session or receipt",
            description = "Requires VIEWER or ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session record",
                    content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid session id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "JWT lacks VIEWER or ADMIN role",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public SessionResponse getSession(@PathVariable @Positive Long id) {
        return chargingSessionService.getSession(id);
    }

    @GetMapping("/users/{userId}/sessions")
    @Operation(
            summary = "List a user's sessions",
            description = "Requires VIEWER or ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session history, newest first",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SessionResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid user id",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "JWT lacks VIEWER or ADMIN role",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<SessionResponse> getUserSessions(@PathVariable @Positive Long userId) {
        return chargingSessionService.getUserSessions(userId);
    }
}
