package com.iwfc.domain.model;

import com.iwfc.domain.exception.UnauthorizedAccessException;

/**
 * Abstraction: what every person in the system is. Each role decides its own permissions,
 * so callers ask a {@code User} what it may do and never test for a concrete class (polymorphism).
 * Fields are private and read through accessors (encapsulation).
 *
 * <p>CMP 7001 mapping: Object-Oriented Principles — Abstraction, Encapsulation and Polymorphism
 * (LO1); role checks raise {@link UnauthorizedAccessException} (LO3, Exception Handling — Unauthorized Access).</p>
 */
public abstract class User {

    private final String id;
    private final String name;
    private boolean active = true;

    protected User(String id, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }
        this.id = id.trim();
        this.name = name.trim();
    }

    public abstract String roleName();

    public abstract Role role();

    public abstract boolean canManageUsers();
    public abstract boolean canManageEquipment();
    public abstract boolean canManageMaintenance();
    public abstract boolean canViewMaintenanceLog();
    public abstract boolean canScheduleSessions();
    public abstract boolean canReportFaults();
    public abstract boolean canLogEquipmentUsage();
    public abstract boolean canBookSessions();
    public abstract boolean canSendReminders();

    public void ensureCanManageUsers() { require(canManageUsers(), "manage user accounts"); }
    public void ensureCanManageEquipment() { require(canManageEquipment(), "manage equipment"); }
    public void ensureCanManageMaintenance() { require(canManageMaintenance(), "manage maintenance requests"); }
    public void ensureCanViewMaintenanceLog() { require(canViewMaintenanceLog(), "view the maintenance log"); }
    public void ensureCanScheduleSessions() { require(canScheduleSessions(), "schedule sessions"); }
    public void ensureCanReportFaults() { require(canReportFaults(), "report faults"); }
    public void ensureCanLogEquipmentUsage() { require(canLogEquipmentUsage(), "log equipment usage"); }
    public void ensureCanSendReminders() { require(canSendReminders(), "send reminders"); }
    public void ensureCanBookSessions() { require(canBookSessions(), "book sessions"); }

    private void require(boolean allowed, String action) {
        if (!allowed) {
            throw new UnauthorizedAccessException(roleName() + " " + name + " is not allowed to " + action);
        }
    }

    public boolean isActive() { return active; }

    /** Switches the account off. History stays; the user can no longer sign in. */
    public void deactivate() { this.active = false; }

    public String id() { return id; }
    public String name() { return name; }

    @Override
    public boolean equals(Object other) {
        return other instanceof User that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return roleName() + " " + name + " (" + id + ")";
    }
}
