package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.repository.Repository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Aggregate root for all fitness sessions. Sessions are only added through it, so the rules
 * (operating hours, no double-booking of studio, instructor or equipment) can never be bypassed.
 * It works over a {@link Repository}, and saves after every change so a database copy stays current.
 */
public class SessionSchedule {

    private final LocalTime opensAt;
    private final LocalTime closesAt;
    private final Repository<FitnessSession, String> sessions;

    public SessionSchedule(LocalTime opensAt, LocalTime closesAt, Repository<FitnessSession, String> sessions) {
        if (opensAt == null || closesAt == null || !closesAt.isAfter(opensAt)) {
            throw new InvalidBookingException("Closing time must be after opening time");
        }
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.sessions = sessions;
    }

    public void schedule(FitnessSession session) {
        validate(session, sessions.findAll());
        sessions.save(session);
    }

    /** Schedules the class for {@code weeks} consecutive weeks, or none at all if any week clashes. */
    public List<FitnessSession> scheduleWeekly(FitnessSession first, int weeks) {
        if (weeks < 1) {
            throw new InvalidBookingException("A recurring class needs at least one week");
        }
        List<FitnessSession> planned = new ArrayList<>(sessions.findAll());
        List<FitnessSession> created = new ArrayList<>();
        for (int week = 0; week < weeks; week++) {
            FitnessSession next = week == 0 ? first : first.repeatedAfterWeeks(week);
            validate(next, planned);
            planned.add(next);
            created.add(next);
        }
        created.forEach(sessions::save);
        return List.copyOf(created);
    }

    public void book(String sessionId, User member) {
        FitnessSession session = require(sessionId);
        session.book(member);
        sessions.save(session);
    }

    public void cancelBooking(String sessionId, User member) {
        FitnessSession session = require(sessionId);
        session.cancel(member);
        sessions.save(session);
    }

    public void cancelSession(String sessionId) {
        require(sessionId);
        sessions.deleteById(sessionId);
    }

    public Optional<FitnessSession> findById(String sessionId) {
        return sessions.findById(sessionId);
    }

    public List<FitnessSession> allSessions() {
        return sessions.findAll().stream().sorted(byStartTime()).toList();
    }

    public List<FitnessSession> availableSessions() {
        return sessions.findAll().stream().filter(session -> !session.isFull()).sorted(byStartTime()).toList();
    }

    private FitnessSession require(String sessionId) {
        return sessions.findById(sessionId)
                .orElseThrow(() -> new InvalidBookingException("No session with id " + sessionId));
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
