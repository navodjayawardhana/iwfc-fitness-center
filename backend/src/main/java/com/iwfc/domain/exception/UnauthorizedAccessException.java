package com.iwfc.domain.exception;

/**
 * Thrown when a user tries an action their role does not allow (e.g. a Member opening the maintenance log).
 *
 * <p>CMP 7001 mapping: Exception Handling (mandatory) — Unauthorized Access (LO3).</p>
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
