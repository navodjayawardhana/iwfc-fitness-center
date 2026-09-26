package com.iwfc.domain.exception;

/** Thrown when a maintenance request is moved through a step the workflow does not allow. */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
