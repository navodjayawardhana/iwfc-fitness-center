package com.iwfc.application.security;

import com.iwfc.domain.model.User;

import java.time.Instant;

/** The result of a successful sign-in: who signed in, the bearer token to send with later requests, and its expiry. */
public record AuthSession(String token, User user, Instant expiresAt) {

    /** The token is a secret, so it is left out of the text form. */
    @Override
    public String toString() {
        return "AuthSession[user=" + user + ", expiresAt=" + expiresAt + "]";
    }
}
