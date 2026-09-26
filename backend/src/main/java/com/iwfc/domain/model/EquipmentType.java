package com.iwfc.domain.model;

/** Kinds of fitness equipment, each with its own preventative-maintenance interval (in usage hours). */
public enum EquipmentType {
    TREADMILL(100),
    SPIN_BIKE(120),
    ROWING_MACHINE(150),
    HEART_RATE_MONITOR(200);

    private final double defaultMaintenanceThresholdHours;

    EquipmentType(double defaultMaintenanceThresholdHours) {
        this.defaultMaintenanceThresholdHours = defaultMaintenanceThresholdHours;
    }

    public double defaultMaintenanceThresholdHours() {
        return defaultMaintenanceThresholdHours;
    }
}
