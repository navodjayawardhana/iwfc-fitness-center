package com.iwfc.domain.exception;

/** Thrown when an operation on equipment breaks a business rule (bad hours, wrong state, blank data). */
public class InvalidEquipmentOperationException extends RuntimeException {

    public InvalidEquipmentOperationException(String message) {
        super(message);
    }
}
