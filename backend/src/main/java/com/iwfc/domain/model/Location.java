package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidEquipmentOperationException;

/** Value Object: where equipment is kept or a session takes place (e.g. "Cardio Zone", "Studio A"). */
public record Location(String name) {

    public Location {
        if (name == null || name.isBlank()) {
            throw new InvalidEquipmentOperationException("A location needs a name");
        }
    }
}
