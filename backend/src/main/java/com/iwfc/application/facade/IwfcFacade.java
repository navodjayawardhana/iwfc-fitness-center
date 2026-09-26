package com.iwfc.application.facade;

import com.iwfc.application.notification.AdminMaintenanceLog;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.usecase.BookSessionUseCase;
import com.iwfc.application.usecase.EquipmentInventoryUseCase;
import com.iwfc.application.usecase.MaintenanceUseCase;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;

import java.util.List;

/**
 * Facade (structural pattern): the single, simple entry point that the console menu and the REST API use.
 * It hides the use cases, repositories and notification wiring behind one small set of methods,
 * so delivery mechanisms never depend on the inner structure of the system.
 */
public class IwfcFacade {

    private final EquipmentInventoryUseCase inventory;
    private final BookSessionUseCase sessions;
    private final MaintenanceUseCase maintenance;
    private final NotificationService notifications;
    private final AdminMaintenanceLog activityLog;
    private final Repository<User, String> users;

    public IwfcFacade(EquipmentInventoryUseCase inventory, BookSessionUseCase sessions, MaintenanceUseCase maintenance,
                      NotificationService notifications, AdminMaintenanceLog activityLog,
                      Repository<User, String> users) {
        this.inventory = inventory;
        this.sessions = sessions;
        this.maintenance = maintenance;
        this.notifications = notifications;
        this.activityLog = activityLog;
        this.users = users;
    }

    // ---- accounts -------------------------------------------------------------------------------

    public User login(String userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user", userId));
    }

    public List<User> listUsers(User actor) {
        actor.ensureCanManageEquipment();
        return users.findAll();
    }

    // ---- equipment ------------------------------------------------------------------------------

    public List<Equipment> listEquipment() {
        return inventory.listAll();
    }

    public Equipment findEquipment(String id) {
        return inventory.find(id);
    }

    public Equipment addEquipment(User actor, EquipmentType type, String id, String name, Location location) {
        return inventory.add(actor, type, id, name, location);
    }

    public void editEquipment(User actor, String id, String name, Location location) {
        inventory.edit(actor, id, name, location);
    }

    public void deactivateEquipment(User actor, String id) {
        inventory.deactivate(actor, id);
    }

    public void logEquipmentUsage(User actor, String id, double hours) {
        inventory.logUsage(actor, id, hours);
    }

    // ---- sessions -------------------------------------------------------------------------------

    public List<FitnessSession> availableSessions() {
        return sessions.availableSessions();
    }

    public List<FitnessSession> allSessions() {
        return sessions.allSessions();
    }

    public FitnessSession findSession(String sessionId) {
        return sessions.find(sessionId);
    }

    public FitnessSession scheduleSession(User instructor, String id, String title, Location studio, TimeSlot slot,
                                          int capacity, List<String> equipmentIds) {
        return sessions.schedule(instructor, id, title, studio, slot, capacity, equipmentIds);
    }

    public List<FitnessSession> scheduleWeeklySession(User instructor, String id, String title, Location studio,
                                                      TimeSlot firstSlot, int capacity, List<String> equipmentIds,
                                                      int weeks) {
        return sessions.scheduleWeekly(instructor, id, title, studio, firstSlot, capacity, equipmentIds, weeks);
    }

    public void bookSession(String sessionId, User member) {
        sessions.book(sessionId, member);
    }

    public void cancelBooking(String sessionId, User member) {
        sessions.cancelBooking(sessionId, member);
    }

    public void cancelSession(User instructor, String sessionId) {
        sessions.cancelSession(instructor, sessionId);
    }

    public void completeSession(User instructor, String sessionId) {
        sessions.completeSession(instructor, sessionId);
    }

    // ---- maintenance ----------------------------------------------------------------------------

    public MaintenanceRequest reportFault(User reporter, String equipmentId, String description, Urgency urgency) {
        return maintenance.report(reporter, equipmentId, description, urgency);
    }

    public void assignMaintenance(User administrator, String requestId, String technician) {
        maintenance.assign(administrator, requestId, technician);
    }

    public void updateMaintenanceProgress(User administrator, String requestId, String note) {
        maintenance.updateProgress(administrator, requestId, note);
    }

    public void completeMaintenance(User administrator, String requestId) {
        maintenance.complete(administrator, requestId);
    }

    public List<MaintenanceRequest> maintenanceRequests(User administrator) {
        return maintenance.allRequests(administrator);
    }

    public List<MaintenanceRequest> myMaintenanceRequests(User reporter) {
        return maintenance.requestsReportedBy(reporter);
    }

    /** The global maintenance log: status changes and preventative alerts, for administrators only. */
    public List<String> maintenanceActivityLog(User administrator) {
        administrator.ensureCanViewMaintenanceLog();
        return activityLog.entries();
    }

    // ---- notifications --------------------------------------------------------------------------

    public List<String> inbox(User user) {
        return notifications.inboxOf(user.id());
    }
}
