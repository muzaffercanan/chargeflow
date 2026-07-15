package com.chargesquare.session.api;

import java.net.URI;
import java.util.List;

import com.chargesquare.session.service.ChargingSessionService;
import com.chargesquare.session.security.RequestActor;
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
public class SessionController {

    private final ChargingSessionService chargingSessionService;

    public SessionController(ChargingSessionService chargingSessionService) {
        this.chargingSessionService = chargingSessionService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> start(
            @Valid @RequestBody StartSessionRequest request,
            Authentication authentication) {
        SessionResponse response = chargingSessionService.start(request, RequestActor.from(authentication));
        return ResponseEntity.created(URI.create("/sessions/" + response.sessionId())).body(response);
    }

    @PostMapping("/sessions/{id}/stop")
    public StopSessionResponse stop(
            @PathVariable @Positive Long id,
            @Valid @RequestBody StopSessionRequest request,
            Authentication authentication) {
        return chargingSessionService.stop(id, request, RequestActor.from(authentication));
    }

    @GetMapping("/sessions/{id}")
    public SessionResponse getSession(@PathVariable @Positive Long id) {
        return chargingSessionService.getSession(id);
    }

    @GetMapping("/users/{userId}/sessions")
    public List<SessionResponse> getUserSessions(@PathVariable @Positive Long userId) {
        return chargingSessionService.getUserSessions(userId);
    }
}
