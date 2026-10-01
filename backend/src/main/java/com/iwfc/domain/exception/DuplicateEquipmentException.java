package com.iwfc.domain.exception;

/**
 * Thrown when equipment is registered with an id that already exists.
 *
 * <p>CMP 7001 mapping: Exception Handling (mandatory) — Duplicate Data (LO3).</p>
 */
public class DuplicateEquipmentException extends RuntimeException {

    public DuplicateEquipmentException(String equipmentId) {
        super("Equipment with id " + equipmentId + " is already registered");
    }
}
