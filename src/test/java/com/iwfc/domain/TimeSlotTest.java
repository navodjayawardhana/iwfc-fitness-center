package com.iwfc.domain;

import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.model.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Tests are written in ZOMBIES order: Zero, One, Many, Boundaries, Interface, Exceptions, Simple. */
class TimeSlotTest {

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 10, 5, hour, minute);
    }

    // Z - Zero
    @Test
    void should_reject_slot_when_duration_is_zero() {
        assertThrows(InvalidBookingException.class, () -> new TimeSlot(at(9, 0), at(9, 0)));
    }

    // O - One
    @Test
    void should_report_duration_when_slot_is_created() {
        TimeSlot slot = new TimeSlot(at(9, 0), at(10, 30));

        assertEquals(Duration.ofMinutes(90), slot.duration());
    }

    // M - Many
    @Test
    void should_detect_overlap_when_slots_partially_intersect() {
        TimeSlot morning = new TimeSlot(at(9, 0), at(10, 0));
        TimeSlot later = new TimeSlot(at(9, 30), at(10, 30));

        assertTrue(morning.overlaps(later));
        assertTrue(later.overlaps(morning));
    }

    @Test
    void should_detect_overlap_when_one_slot_contains_the_other() {
        TimeSlot outer = new TimeSlot(at(9, 0), at(12, 0));
        TimeSlot inner = new TimeSlot(at(10, 0), at(11, 0));

        assertTrue(outer.overlaps(inner));
        assertTrue(inner.overlaps(outer));
    }

    // B - Boundaries
    @Test
    void should_not_overlap_when_one_slot_ends_exactly_when_the_other_starts() {
        TimeSlot first = new TimeSlot(at(9, 0), at(10, 0));
        TimeSlot second = new TimeSlot(at(10, 0), at(11, 0));

        assertFalse(first.overlaps(second));
        assertFalse(second.overlaps(first));
    }

    @Test
    void should_overlap_when_slots_are_identical() {
        TimeSlot a = new TimeSlot(at(9, 0), at(10, 0));
        TimeSlot b = new TimeSlot(at(9, 0), at(10, 0));

        assertTrue(a.overlaps(b));
    }

    // I - Interface (value object contract: equality by value)
    @Test
    void should_be_equal_when_start_and_end_are_the_same() {
        assertEquals(new TimeSlot(at(9, 0), at(10, 0)), new TimeSlot(at(9, 0), at(10, 0)));
    }

    // E - Exceptions
    @Test
    void should_reject_slot_when_end_is_before_start() {
        assertThrows(InvalidBookingException.class, () -> new TimeSlot(at(10, 0), at(9, 0)));
    }

    @Test
    void should_reject_slot_when_start_or_end_is_missing() {
        assertThrows(InvalidBookingException.class, () -> new TimeSlot(null, at(9, 0)));
        assertThrows(InvalidBookingException.class, () -> new TimeSlot(at(9, 0), null));
    }

    // S - Simple / happy path
    @Test
    void should_not_overlap_when_slots_are_on_different_times() {
        TimeSlot morning = new TimeSlot(at(9, 0), at(10, 0));
        TimeSlot evening = new TimeSlot(at(18, 0), at(19, 0));

        assertFalse(morning.overlaps(evening));
    }
}
