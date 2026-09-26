package com.iwfc.domain.exception;

/** Thrown when an equipment item, session or maintenance request with the given id does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String kind, String id) {
        super("No " + kind + " found with id " + id);
    }
}
