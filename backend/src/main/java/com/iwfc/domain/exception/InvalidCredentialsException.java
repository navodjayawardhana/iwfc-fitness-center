package com.iwfc.domain.exception;

/**
 * Thrown when sign-in fails or a token is missing, unknown or expired.
 * The default message is the same for a wrong password and an unknown user, so it never reveals which ids exist.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid user id or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
