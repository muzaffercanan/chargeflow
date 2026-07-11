package com.chargesquare.session.exception;

import org.springframework.http.HttpStatus;

public class SessionNotActiveException extends ApiException {
    public SessionNotActiveException(Long sessionId) {
        super(HttpStatus.CONFLICT, "SESSION_NOT_ACTIVE", "Session " + sessionId + " is not ACTIVE");
    }
}

