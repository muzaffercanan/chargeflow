package com.chargesquare.session.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(Long userId) {
        super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User " + userId + " was not found");
    }
}

