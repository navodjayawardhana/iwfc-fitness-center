package com.iwfc.domain.exception;

/** Thrown when equipment is registered with an id that already exists. */
public class DuplicateEquipmentException extends RuntimeException {

    public DuplicateEquipmentException(String equipmentId) {
        super("Equipment with id " + equipmentId + " is already registered");
    }
}
