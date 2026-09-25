package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidEquipmentOperationException;

/**
 * Factory (creational pattern): builds Equipment with the maintenance interval that suits its type,
 * so callers never have to know those defaults.
 */
public class EquipmentFactory {

    public Equipment create(EquipmentType type, String id, String name, Location location) {
        requireType(type);
        return create(type, id, name, location, type.defaultMaintenanceThresholdHours());
    }

    public Equipment create(EquipmentType type, String id, String name, Location location, double thresholdHours) {
        requireType(type);
        return new Equipment(id, name, type, location, thresholdHours);
    }

    private static void requireType(EquipmentType type) {
        if (type == null) {
            throw new InvalidEquipmentOperationException("Equipment type is required");
        }
    }
}
