package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidEquipmentOperationException;

import java.util.Objects;

/**
 * Entity (aggregate root): one piece of fitness equipment, identified by its unique id.
 * It owns its own rules: usage tracking, the preventative maintenance alert and status changes.
 * The constructor is package-private so equipment is created through the equipment factory.
 */
public class Equipment {

    private final String id;
    private final EquipmentType type;
    private final double maintenanceThresholdHours;
    private String name;
    private Location location;
    private EquipmentStatus status = EquipmentStatus.OPERATIONAL;
    private boolean active = true;
    private double totalUsageHours;
    private double hoursSinceMaintenance;

    Equipment(String id, String name, EquipmentType type, Location location, double maintenanceThresholdHours) {
        this.id = requireText(id, "Equipment id");
        this.name = requireText(name, "Equipment name");
        this.type = Objects.requireNonNull(type, "Equipment type is required");
        this.location = Objects.requireNonNull(location, "Equipment location is required");
        if (maintenanceThresholdHours <= 0) {
            throw new InvalidEquipmentOperationException("Maintenance threshold must be greater than zero");
        }
        this.maintenanceThresholdHours = maintenanceThresholdHours;
    }

    public void logUsage(double hours) {
        if (hours <= 0) {
            throw new InvalidEquipmentOperationException("Usage hours must be greater than zero");
        }
        if (!isAvailableForSessions()) {
            throw new InvalidEquipmentOperationException("Equipment " + id + " cannot be used right now");
        }
        totalUsageHours += hours;
        hoursSinceMaintenance += hours;
    }

    public boolean needsMaintenance() {
        return hoursSinceMaintenance >= maintenanceThresholdHours;
    }

    public void markFaulty() {
        status = EquipmentStatus.FAULTY;
    }

    public void startMaintenance() {
        status = EquipmentStatus.UNDER_MAINTENANCE;
    }

    public void completeMaintenance() {
        if (status != EquipmentStatus.UNDER_MAINTENANCE) {
            throw new InvalidEquipmentOperationException("Equipment " + id + " is not under maintenance");
        }
        status = EquipmentStatus.OPERATIONAL;
        hoursSinceMaintenance = 0;
    }

    public void deactivate() {
        active = false;
    }

    public void rename(String newName) {
        this.name = requireText(newName, "Equipment name");
    }

    public void relocate(Location newLocation) {
        this.location = Objects.requireNonNull(newLocation, "Equipment location is required");
    }

    public boolean isAvailableForSessions() {
        return active && status == EquipmentStatus.OPERATIONAL;
    }

    public String id() { return id; }
    public String name() { return name; }
    public EquipmentType type() { return type; }
    public Location location() { return location; }
    public EquipmentStatus status() { return status; }
    public boolean isActive() { return active; }
    public double totalUsageHours() { return totalUsageHours; }
    public double hoursSinceMaintenance() { return hoursSinceMaintenance; }
    public double maintenanceThresholdHours() { return maintenanceThresholdHours; }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidEquipmentOperationException(label + " is required");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Equipment that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + name + " [" + status + ", " + location.name() + "]";
    }
}
