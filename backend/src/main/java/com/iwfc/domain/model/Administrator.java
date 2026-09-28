package com.iwfc.domain.model;

/** Manages equipment inventory, user accounts and the global maintenance log. */
public class Administrator extends User {

    public Administrator(String id, String name) { super(id, name); }

    @Override public String roleName() { return "Administrator"; }
    @Override public Role role() { return Role.ADMINISTRATOR; }
    @Override public boolean canManageUsers() { return true; }
    @Override public boolean canManageEquipment() { return true; }
    @Override public boolean canManageMaintenance() { return true; }
    @Override public boolean canViewMaintenanceLog() { return true; }
    @Override public boolean canScheduleSessions() { return false; }
    @Override public boolean canReportFaults() { return false; }
    @Override public boolean canLogEquipmentUsage() { return false; }
    @Override public boolean canSendReminders() { return true; }
    @Override public boolean canBookSessions() { return false; }
}
