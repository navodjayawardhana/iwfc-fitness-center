package com.iwfc.application.usecase;

import com.iwfc.application.notification.NotificationService;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.SessionSchedule;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;

import java.util.List;

/** Orchestrates scheduling and booking. Clash and capacity rules are enforced by the domain. */
public class BookSessionUseCase {

    private final SessionSchedule schedule;
    private final Repository<Equipment, String> equipment;
    private final EquipmentInventoryUseCase inventory;
    private final NotificationService notifications;

    public BookSessionUseCase(SessionSchedule schedule, Repository<Equipment, String> equipment,
                              EquipmentInventoryUseCase inventory, NotificationService notifications) {
        this.schedule = schedule;
        this.equipment = equipment;
        this.inventory = inventory;
        this.notifications = notifications;
    }

    public FitnessSession schedule(User instructor, String id, String title, Location studio, TimeSlot slot,
                                   int capacity, List<String> equipmentIds) {
        FitnessSession session = new FitnessSession(id, title, instructor, studio, slot, capacity,
                resolve(equipmentIds));
        schedule.schedule(session);
        return session;
    }

    public List<FitnessSession> scheduleWeekly(User instructor, String id, String title, Location studio,
                                               TimeSlot firstSlot, int capacity, List<String> equipmentIds, int weeks) {
        FitnessSession first = new FitnessSession(id, title, instructor, studio, firstSlot, capacity,
                resolve(equipmentIds));
        return schedule.scheduleWeekly(first, weeks);
    }

    public List<FitnessSession> availableSessions() {
        return schedule.availableSessions();
    }

    public List<FitnessSession> allSessions() {
        return schedule.allSessions();
    }

    public FitnessSession find(String sessionId) {
        return schedule.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("session", sessionId));
    }

    public void book(String sessionId, User member) {
        FitnessSession session = find(sessionId);
        session.book(member);
        notifications.send(member.id(),
                "Booked: " + session.title() + " on " + session.slot().start() + " in " + session.studio().name());
    }

    public void cancelBooking(String sessionId, User member) {
        find(sessionId).cancel(member);
    }

    public void cancelSession(User instructor, String sessionId) {
        instructor.ensureCanScheduleSessions();
        FitnessSession session = find(sessionId);
        schedule.cancelSession(sessionId);
        for (User member : session.bookedMembers()) {
            notifications.send(member.id(), "Your session was cancelled: " + session.title() + " on " + session.slot().start());
        }
    }

    /** Records the session's length as usage hours on every piece of equipment it used. */
    public void completeSession(User instructor, String sessionId) {
        instructor.ensureCanLogEquipmentUsage();
        FitnessSession session = find(sessionId);
        double hours = session.slot().duration().toMinutes() / 60.0;
        for (Equipment item : session.equipment()) {
            inventory.logUsage(instructor, item.id(), hours);
        }
    }

    private List<Equipment> resolve(List<String> equipmentIds) {
        return equipmentIds.stream()
                .map(id -> equipment.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("equipment", id)))
                .toList();
    }
}
