package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidBookingException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Value Object: a period of time defined only by its start and end.
 * Immutable, and equal to any other slot with the same start and end.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        if (start == null || end == null) {
            throw new InvalidBookingException("A time slot needs both a start and an end time");
        }
        if (!end.isAfter(start)) {
            throw new InvalidBookingException("A time slot must end after it starts");
        }
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    /** Slots that only touch (one ends exactly when the other starts) do not overlap. */
    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
