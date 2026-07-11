package com.chargesquare.session.exception;

import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApiException {
    public SessionNotFoundException(Long sessionId) {
        super(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session " + sessionId + " was not found");
    }
}

