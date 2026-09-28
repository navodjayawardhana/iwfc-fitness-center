package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.exception.InvalidCredentialsException;
import com.iwfc.domain.model.User;

/** Reads the bearer token from the Authorization header and asks the facade who it belongs to. */
final class ApiAuth {

    private static final String PREFIX = "bearer ";

    private ApiAuth() {
    }

    static User user(IwfcFacade system, String authorization) {
        return system.authenticate(token(authorization));
    }

    /** Returns the token part of {@code Bearer <token>}, or fails when the header has another shape. */
    static String token(String authorization) {
        if (authorization == null || authorization.length() <= PREFIX.length()
                || !authorization.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            throw new InvalidCredentialsException("Send the token as: Authorization: Bearer <token>");
        }
        return authorization.substring(PREFIX.length()).trim();
    }
}
