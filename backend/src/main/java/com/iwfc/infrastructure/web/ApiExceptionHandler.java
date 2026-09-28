package com.iwfc.infrastructure.web;

import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.DuplicateUserException;
import com.iwfc.domain.exception.InvalidCredentialsException;
import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.InvalidEquipmentOperationException;
import com.iwfc.domain.exception.InvalidStatusTransitionException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;

/** Turns the domain's custom exceptions into HTTP status codes, in one place. */
@RestControllerAdvice
public class ApiExceptionHandler {

    public record ErrorBody(String error, String message) { }

    @ExceptionHandler(UnauthorizedAccessException.class)
    ResponseEntity<ErrorBody> forbidden(UnauthorizedAccessException error) {
        return reply(HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACCESS", error);
    }

    @ExceptionHandler(InvalidBookingException.class)
    ResponseEntity<ErrorBody> invalidBooking(InvalidBookingException error) {
        return reply(HttpStatus.CONFLICT, "INVALID_BOOKING", error);
    }

    @ExceptionHandler({DuplicateEquipmentException.class, DuplicateUserException.class})
    ResponseEntity<ErrorBody> duplicate(RuntimeException error) {
        return reply(HttpStatus.CONFLICT, "DUPLICATE", error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorBody> invalidCredentials(InvalidCredentialsException error) {
        return reply(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", error);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ResponseEntity<ErrorBody> invalidTransition(InvalidStatusTransitionException error) {
        return reply(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorBody> notFound(ResourceNotFoundException error) {
        return reply(HttpStatus.NOT_FOUND, "NOT_FOUND", error);
    }

    @ExceptionHandler({InvalidEquipmentOperationException.class, IllegalArgumentException.class,
            DateTimeParseException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorBody> badRequest(Exception error) {
        return reply(HttpStatus.BAD_REQUEST, "BAD_REQUEST", error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorBody> missingUser(MissingRequestHeaderException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorBody("MISSING_TOKEN",
                        "Sign in at POST /api/login, then send the token in the " + error.getHeaderName()
                                + " header as: Bearer <token>"));
    }

    private static ResponseEntity<ErrorBody> reply(HttpStatus status, String code, Exception error) {
        return ResponseEntity.status(status).body(new ErrorBody(code, error.getMessage()));
    }
}
