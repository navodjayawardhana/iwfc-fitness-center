package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** ZOMBIES order: Zero, One, Many, Boundaries, Interface, Exceptions, Simple. */
class FitnessSessionTest {

    private final EquipmentFactory factory = new EquipmentFactory();
    private final Instructor instructor = new Instructor("I-1", "Nushfa");
    private final Location studioA = new Location("Studio A");

    private static TimeSlot slot(int startHour, int endHour) {
        return new TimeSlot(LocalDateTime.of(2026, 10, 5, startHour, 0), LocalDateTime.of(2026, 10, 5, endHour, 0));
    }

    private FitnessSession yoga(int capacity) {
        return new FitnessSession("S-1", "Morning Yoga", instructor, studioA, slot(9, 10), capacity, List.of());
    }

    // Z - Zero
    @Test
    void should_start_with_no_bookings_when_session_is_created() {
        FitnessSession session = yoga(10);

        assertEquals(0, session.bookedCount());
        assertEquals(10, session.availableSpots());
        assertFalse(session.isFull());
    }

    // O - One
    @Test
    void should_reduce_available_spots_when_one_member_books() {
        FitnessSession session = yoga(10);

        session.book(new Member("M-1", "Supun"));

        assertEquals(1, session.bookedCount());
        assertEquals(9, session.availableSpots());
    }

    // M - Many
    @Test
    void should_hold_many_members_when_several_book() {
        FitnessSession session = yoga(10);

        session.book(new Member("M-1", "Supun"));
        session.book(new Member("M-2", "Navod"));
        session.book(new Member("M-3", "Nushfa"));

        assertEquals(3, session.bookedCount());
    }

    // B - Boundaries
    @Test
    void should_be_full_when_the_last_spot_is_booked() {
        FitnessSession session = yoga(2);
        session.book(new Member("M-1", "Supun"));

        session.book(new Member("M-2", "Navod"));

        assertTrue(session.isFull());
        assertEquals(0, session.availableSpots());
    }

    @Test
    void should_free_a_spot_when_a_booking_is_cancelled() {
        FitnessSession session = yoga(1);
        Member member = new Member("M-1", "Supun");
        session.book(member);

        session.cancel(member);

        assertFalse(session.isFull());
        assertEquals(0, session.bookedCount());
    }

    // I - Interface (conflict rules the schedule relies on)
    @Test
    void should_conflict_when_same_studio_and_times_overlap() {
        FitnessSession first = yoga(10);
        FitnessSession second = new FitnessSession("S-2", "HIIT", new Instructor("I-2", "Other"), studioA,
                slot(9, 11), 10, List.of());

        assertTrue(first.conflictsWith(second));
        assertTrue(second.conflictsWith(first));
    }

    @Test
    void should_conflict_when_the_same_equipment_is_needed_at_overlapping_times() {
        Equipment bike = factory.create(EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", new Location("Cardio Zone"));
        FitnessSession first = new FitnessSession("S-1", "Spin", instructor, studioA, slot(9, 10), 10, List.of(bike));
        FitnessSession second = new FitnessSession("S-2", "Spin Advanced", new Instructor("I-2", "Other"),
                new Location("Studio B"), slot(9, 10), 10, List.of(bike));

        assertTrue(first.conflictsWith(second));
    }

    @Test
    void should_conflict_when_the_same_instructor_teaches_two_overlapping_sessions() {
        FitnessSession first = yoga(10);
        FitnessSession second = new FitnessSession("S-2", "Pilates", instructor, new Location("Studio B"),
                slot(9, 10), 10, List.of());

        assertTrue(first.conflictsWith(second));
    }

    @Test
    void should_not_conflict_when_sessions_are_back_to_back_in_the_same_studio() {
        FitnessSession first = yoga(10);
        FitnessSession second = new FitnessSession("S-2", "HIIT", instructor, studioA, slot(10, 11), 10, List.of());

        assertFalse(first.conflictsWith(second));
    }

    @Test
    void should_not_conflict_when_different_studio_equipment_and_instructor_share_a_time() {
        FitnessSession first = yoga(10);
        FitnessSession second = new FitnessSession("S-2", "HIIT", new Instructor("I-2", "Other"),
                new Location("Studio B"), slot(9, 10), 10, List.of());

        assertFalse(first.conflictsWith(second));
    }

    // E - Exceptions
    @Test
    void should_reject_booking_when_session_is_full() {
        FitnessSession session = yoga(1);
        session.book(new Member("M-1", "Supun"));

        assertThrows(InvalidBookingException.class, () -> session.book(new Member("M-2", "Navod")));
    }

    @Test
    void should_reject_booking_when_member_is_already_booked() {
        FitnessSession session = yoga(5);
        Member member = new Member("M-1", "Supun");
        session.book(member);

        assertThrows(InvalidBookingException.class, () -> session.book(member));
    }

    @Test
    void should_reject_booking_when_user_is_not_a_member() {
        FitnessSession session = yoga(5);

        assertThrows(UnauthorizedAccessException.class, () -> session.book(new Administrator("A-1", "Prasad")));
    }

    @Test
    void should_reject_cancelling_when_member_has_no_booking() {
        assertThrows(InvalidBookingException.class, () -> yoga(5).cancel(new Member("M-9", "Nobody")));
    }

    @Test
    void should_reject_session_when_scheduled_by_a_non_instructor() {
        Member notAnInstructor = new Member("M-1", "Supun");

        assertThrows(UnauthorizedAccessException.class,
                () -> new FitnessSession("S-1", "Yoga", notAnInstructor, studioA, slot(9, 10), 10, List.of()));
    }

    @Test
    void should_reject_session_when_capacity_is_not_positive() {
        assertThrows(InvalidBookingException.class, () -> yoga(0));
    }

    @Test
    void should_reject_session_when_equipment_is_faulty() {
        Equipment bike = factory.create(EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", new Location("Cardio Zone"));
        bike.markFaulty();

        assertThrows(InvalidBookingException.class,
                () -> new FitnessSession("S-1", "Spin", instructor, studioA, slot(9, 10), 10, List.of(bike)));
    }

    // S - Simple
    @Test
    void should_create_the_same_session_one_week_later_when_repeated() {
        FitnessSession session = yoga(10);

        FitnessSession nextWeek = session.repeatedAfterWeeks(1);

        assertEquals("S-1-w1", nextWeek.id());
        assertEquals(session.slot().start().plusWeeks(1), nextWeek.slot().start());
        assertEquals(session.slot().duration(), nextWeek.slot().duration());
        assertEquals(0, nextWeek.bookedCount());
    }
}
