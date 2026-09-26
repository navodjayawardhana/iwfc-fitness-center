package com.iwfc.domain.exception;

/** Thrown when a booking or time slot breaks a business rule (double-booking, bad times, outside hours). */
public class InvalidBookingException extends RuntimeException {

    public InvalidBookingException(String message) {
        super(message);
    }
}
