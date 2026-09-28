package com.iwfc.domain.exception;

/** Thrown when a user account is registered with an id that already exists. */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String userId) {
        super("User with id " + userId + " is already registered");
    }
}
