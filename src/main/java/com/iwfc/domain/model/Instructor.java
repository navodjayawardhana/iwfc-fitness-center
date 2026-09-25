package com.iwfc.domain.model;

/** Staff who schedule sessions, track equipment usage and report faults. */
public class Instructor extends User {

    public Instructor(String id, String name) { super(id, name); }

    @Override public String roleName() { return "Instructor"; }
    @Override public boolean canManageEquipment() { return false; }
    @Override public boolean canManageMaintenance() { return false; }
    @Override public boolean canViewMaintenanceLog() { return false; }
    @Override public boolean canScheduleSessions() { return true; }
    @Override public boolean canReportFaults() { return true; }
    @Override public boolean canLogEquipmentUsage() { return true; }
    @Override public boolean canBookSessions() { return false; }
}
