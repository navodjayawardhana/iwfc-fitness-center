package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidBookingException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Aggregate root for all fitness sessions. Sessions are only added through it, so the rules
 * (operating hours, no double-booking of studio, instructor or equipment) can never be bypassed.
 */
public class SessionSchedule {

    private final LocalTime opensAt;
    private final LocalTime closesAt;
    private final List<FitnessSession> sessions = new ArrayList<>();

    public SessionSchedule(LocalTime opensAt, LocalTime closesAt) {
        if (opensAt == null || closesAt == null || !closesAt.isAfter(opensAt)) {
            throw new InvalidBookingException("Closing time must be after opening time");
        }
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public void schedule(FitnessSession session) {
        validate(session, sessions);
        sessions.add(session);
    }

    /** Schedules the class for {@code weeks} consecutive weeks, or none at all if any week clashes. */
    public List<FitnessSession> scheduleWeekly(FitnessSession first, int weeks) {
        if (weeks < 1) {
            throw new InvalidBookingException("A recurring class needs at least one week");
        }
        List<FitnessSession> planned = new ArrayList<>(sessions);
        List<FitnessSession> created = new ArrayList<>();
        for (int week = 0; week < weeks; week++) {
            FitnessSession next = week == 0 ? first : first.repeatedAfterWeeks(week);
            validate(next, planned);
            planned.add(next);
            created.add(next);
        }
        sessions.addAll(created);
        return List.copyOf(created);
    }

    public void book(String sessionId, User member) {
        FitnessSession session = findById(sessionId)
                .orElseThrow(() -> new InvalidBookingException("No session with id " + sessionId));
        session.book(member);
    }

    public void cancelSession(String sessionId) {
        if (!sessions.removeIf(session -> session.id().equals(sessionId))) {
            throw new InvalidBookingException("No session with id " + sessionId);
        }
    }

    public Optional<FitnessSession> findById(String sessionId) {
        return sessions.stream().filter(session -> session.id().equals(sessionId)).findFirst();
    }

    public List<FitnessSession> allSessions() {
        return sessions.stream().sorted(byStartTime()).toList();
    }

    public List<FitnessSession> availableSessions() {
        return sessions.stream().filter(session -> !session.isFull()).sorted(byStartTime()).toList();
    }

    private void validate(FitnessSession candidate, List<FitnessSession> existing) {
        TimeSlot slot = candidate.slot();
        boolean sameDay = slot.start().toLocalDate().equals(slot.end().toLocalDate());
        if (!sameDay || slot.start().toLocalTime().isBefore(opensAt) || slot.end().toLocalTime().isAfter(closesAt)) {
            throw new InvalidBookingException("Sessions must be held between " + opensAt + " and " + closesAt);
        }
        for (FitnessSession other : existing) {
            if (other.id().equals(candidate.id())) {
                throw new InvalidBookingException("A session with id " + candidate.id() + " already exists");
            }
            if (candidate.conflictsWith(other)) {
                throw new InvalidBookingException(
                        candidate.id() + " clashes with " + other.id() + " (studio, instructor or equipment already booked)");
            }
        }
    }

    private static Comparator<FitnessSession> byStartTime() {
        return Comparator.comparing(session -> session.slot().start());
    }
}
