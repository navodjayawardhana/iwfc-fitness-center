package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidBookingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entity: one scheduled class (e.g. "Morning Yoga") led by an instructor, held in a studio at a time,
 * optionally using equipment, with a limited number of spots.
 * It guards its own booking rules and knows when it clashes with another session.
 */
public class FitnessSession {

    private final String id;
    private final String title;
    private final User instructor;
    private final Location studio;
    private final TimeSlot slot;
    private final int capacity;
    private final List<Equipment> equipment;
    private final List<User> bookedMembers = new ArrayList<>();

    public FitnessSession(String id, String title, User instructor, Location studio, TimeSlot slot,
                          int capacity, List<Equipment> equipment) {
        this.instructor = Objects.requireNonNull(instructor, "A session needs an instructor");
        instructor.ensureCanScheduleSessions();
        if (id == null || id.isBlank() || title == null || title.isBlank()) {
            throw new InvalidBookingException("A session needs an id and a title");
        }
        if (capacity <= 0) {
            throw new InvalidBookingException("A session needs at least one spot");
        }
        this.id = id.trim();
        this.title = title.trim();
        this.studio = Objects.requireNonNull(studio, "A session needs a studio");
        this.slot = Objects.requireNonNull(slot, "A session needs a time slot");
        this.capacity = capacity;
        this.equipment = List.copyOf(equipment);
        for (Equipment item : this.equipment) {
            if (!item.isAvailableForSessions()) {
                throw new InvalidBookingException("Equipment " + item.id() + " is not available for sessions");
            }
        }
    }

    public void book(User member) {
        member.ensureCanBookSessions();
        if (bookedMembers.contains(member)) {
            throw new InvalidBookingException(member.name() + " is already booked into " + title);
        }
        if (isFull()) {
            throw new InvalidBookingException(title + " is fully booked");
        }
        bookedMembers.add(member);
    }

    public void cancel(User member) {
        if (!bookedMembers.remove(member)) {
            throw new InvalidBookingException(member.name() + " has no booking in " + title);
        }
    }

    public boolean isFull() {
        return bookedMembers.size() >= capacity;
    }

    public int bookedCount() {
        return bookedMembers.size();
    }

    public int availableSpots() {
        return capacity - bookedMembers.size();
    }

    /** A clash needs overlapping times plus something shared: the studio, the instructor or a piece of equipment. */
    public boolean conflictsWith(FitnessSession other) {
        if (!slot.overlaps(other.slot)) {
            return false;
        }
        return studio.equals(other.studio)
                || instructor.equals(other.instructor)
                || equipment.stream().anyMatch(other.equipment::contains);
    }

    /** The same class one or more weeks later, with no bookings (used for recurring weekly classes). */
    public FitnessSession repeatedAfterWeeks(int weeks) {
        TimeSlot later = new TimeSlot(slot.start().plusWeeks(weeks), slot.end().plusWeeks(weeks));
        return new FitnessSession(id + "-w" + weeks, title, instructor, studio, later, capacity, equipment);
    }

    public String id() { return id; }
    public String title() { return title; }
    public User instructor() { return instructor; }
    public Location studio() { return studio; }
    public TimeSlot slot() { return slot; }
    public int capacity() { return capacity; }
    public List<Equipment> equipment() { return equipment; }
    public List<User> bookedMembers() { return Collections.unmodifiableList(bookedMembers); }

    @Override
    public boolean equals(Object other) {
        return other instanceof FitnessSession that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + title + " " + slot.start() + " @ " + studio.name()
                + " (" + bookedMembers.size() + "/" + capacity + ")";
    }
}
