package com.iwfc.domain.model;

/** A client who views the schedule, books sessions and receives notifications. */
public class Member extends User {

    public Member(String id, String name) { super(id, name); }

    @Override public String roleName() { return "Member"; }
    @Override public Role role() { return Role.MEMBER; }
    @Override public boolean canManageUsers() { return false; }
    @Override public boolean canManageEquipment() { return false; }
    @Override public boolean canManageMaintenance() { return false; }
    @Override public boolean canViewMaintenanceLog() { return false; }
    @Override public boolean canScheduleSessions() { return false; }
    @Override public boolean canReportFaults() { return false; }
    @Override public boolean canLogEquipmentUsage() { return false; }
    @Override public boolean canBookSessions() { return true; }
}
