package com.iwfc.domain.model;

import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import org.junit.jupiter.api.Test;

import com.iwfc.support.CopyingSessionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The schedule is the aggregate root that keeps sessions free of double-bookings. */
class SessionScheduleTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    private final SessionSchedule schedule = new SessionSchedule(LocalTime.of(6, 0), LocalTime.of(22, 0), new InMemoryRepository<>(FitnessSession::id));
    private final EquipmentFactory factory = new EquipmentFactory();
    private final Instructor instructor = new Instructor("I-1", "Nushfa");
    private final Location studioA = new Location("Studio A");

    private static TimeSlot slot(int startHour, int endHour) {
        return new TimeSlot(MONDAY.atTime(startHour, 0), MONDAY.atTime(endHour, 0));
    }

    private FitnessSession session(String id, Instructor by, Location where, TimeSlot when, Equipment... equipment) {
        return new FitnessSession(id, "Class " + id, by, where, when, 10, List.of(equipment));
    }

    // Z
    @Test
    void should_have_no_sessions_when_new() {
        assertTrue(schedule.allSessions().isEmpty());
        assertTrue(schedule.availableSessions().isEmpty());
    }

    // O
    @Test
    void should_store_the_session_when_one_is_scheduled() {
        FitnessSession yoga = session("S-1", instructor, studioA, slot(9, 10));

        schedule.schedule(yoga);

        assertEquals(List.of(yoga), schedule.allSessions());
        assertEquals(Optional.of(yoga), schedule.findById("S-1"));
    }

    // M
    @Test
    void should_list_sessions_in_start_time_order_when_many_are_scheduled() {
        FitnessSession evening = session("S-2", instructor, studioA, slot(18, 19));
        FitnessSession morning = session("S-1", instructor, studioA, slot(9, 10));
        schedule.schedule(evening);
        schedule.schedule(morning);

        assertEquals(List.of(morning, evening), schedule.allSessions());
    }

    @Test
    void should_only_list_sessions_with_free_spots_as_available() {
        FitnessSession full = new FitnessSession("S-1", "Tiny", instructor, studioA, slot(9, 10), 1, List.of());
        FitnessSession open = session("S-2", instructor, studioA, slot(11, 12));
        schedule.schedule(full);
        schedule.schedule(open);
        full.book(new Member("M-1", "Supun"));

        assertEquals(List.of(open), schedule.availableSessions());
    }

    // B - operating hours boundaries
    @Test
    void should_accept_a_session_that_fills_the_operating_hours_exactly() {
        assertDoesNotThrow(() -> schedule.schedule(session("S-1", instructor, studioA, slot(6, 22))));
    }

    @Test
    void should_reject_a_session_that_starts_before_opening_time() {
        FitnessSession early = session("S-1", instructor, studioA,
                new TimeSlot(MONDAY.atTime(5, 30), MONDAY.atTime(6, 30)));

        assertThrows(InvalidBookingException.class, () -> schedule.schedule(early));
    }

    @Test
    void should_reject_a_session_that_ends_after_closing_time() {
        FitnessSession late = session("S-1", instructor, studioA,
                new TimeSlot(MONDAY.atTime(21, 30), MONDAY.atTime(22, 30)));

        assertThrows(InvalidBookingException.class, () -> schedule.schedule(late));
    }

    @Test
    void should_accept_back_to_back_sessions_in_the_same_studio() {
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));

        assertDoesNotThrow(() -> schedule.schedule(session("S-2", instructor, studioA, slot(10, 11))));
    }

    // I - Interface (booking through the schedule)
    @Test
    void should_book_a_member_into_a_session_by_id() {
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));
        Member member = new Member("M-1", "Supun");

        schedule.book("S-1", member);

        assertEquals(1, schedule.findById("S-1").orElseThrow().bookedCount());
    }

    @Test
    void should_cancel_a_session_and_remove_it_from_the_schedule() {
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));

        schedule.cancelSession("S-1");

        assertTrue(schedule.findById("S-1").isEmpty());
    }

    // E - Exceptions
    @Test
    void should_reject_double_booking_of_the_same_studio() {
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));
        FitnessSession clash = session("S-2", new Instructor("I-2", "Other"), studioA, slot(9, 11));

        InvalidBookingException error = assertThrows(InvalidBookingException.class, () -> schedule.schedule(clash));

        assertTrue(error.getMessage().contains("S-1"));
    }

    @Test
    void should_reject_double_booking_of_the_same_equipment() {
        Equipment bike = factory.create(EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", new Location("Cardio Zone"));
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10), bike));
        FitnessSession clash = session("S-2", new Instructor("I-2", "Other"), new Location("Studio B"), slot(9, 10), bike);

        assertThrows(InvalidBookingException.class, () -> schedule.schedule(clash));
    }

    @Test
    void should_reject_a_duplicate_session_id() {
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));

        assertThrows(InvalidBookingException.class,
                () -> schedule.schedule(session("S-1", instructor, new Location("Studio B"), slot(12, 13))));
    }

    @Test
    void should_reject_booking_for_an_unknown_session() {
        assertThrows(InvalidBookingException.class, () -> schedule.book("nope", new Member("M-1", "Supun")));
    }

    @Test
    void should_reject_booking_by_a_non_member() {
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));

        assertThrows(UnauthorizedAccessException.class, () -> schedule.book("S-1", instructor));
    }

    @Test
    void should_reject_operating_hours_when_closing_is_not_after_opening() {
        assertThrows(InvalidBookingException.class,
                () -> new SessionSchedule(LocalTime.of(22, 0), LocalTime.of(6, 0), new InMemoryRepository<>(FitnessSession::id)));
    }

    // Persistence: the schedule works over a repository, and a database only keeps what was saved
    private SessionSchedule databaseBackedSchedule(CopyingSessionRepository database) {
        return new SessionSchedule(LocalTime.of(6, 0), LocalTime.of(22, 0), database);
    }

    @Test
    void should_save_a_session_when_it_is_scheduled() {
        CopyingSessionRepository database = new CopyingSessionRepository();

        databaseBackedSchedule(database).schedule(session("S-1", instructor, studioA, slot(9, 10)));

        assertTrue(database.existsById("S-1"));
    }

    @Test
    void should_save_a_booking_so_it_is_still_there_when_the_session_is_loaded_again() {
        CopyingSessionRepository database = new CopyingSessionRepository();
        SessionSchedule schedule = databaseBackedSchedule(database);
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));

        schedule.book("S-1", new Member("M-1", "Supun"));

        assertEquals(1, database.findById("S-1").orElseThrow().bookedCount());
    }

    @Test
    void should_save_a_cancelled_booking() {
        CopyingSessionRepository database = new CopyingSessionRepository();
        SessionSchedule schedule = databaseBackedSchedule(database);
        Member member = new Member("M-1", "Supun");
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));
        schedule.book("S-1", member);

        schedule.cancelBooking("S-1", member);

        assertEquals(0, database.findById("S-1").orElseThrow().bookedCount());
    }

    @Test
    void should_remove_a_cancelled_session_from_the_database() {
        CopyingSessionRepository database = new CopyingSessionRepository();
        SessionSchedule schedule = databaseBackedSchedule(database);
        schedule.schedule(session("S-1", instructor, studioA, slot(9, 10)));

        schedule.cancelSession("S-1");

        assertFalse(database.existsById("S-1"));
    }

    @Test
    void should_save_every_session_of_a_weekly_class() {
        CopyingSessionRepository database = new CopyingSessionRepository();

        databaseBackedSchedule(database).scheduleWeekly(session("PIL", instructor, studioA, slot(7, 8)), 3);

        assertEquals(3, database.count());
    }

    @Test
    void should_see_sessions_that_were_already_stored_when_it_starts() {
        CopyingSessionRepository database = new CopyingSessionRepository();
        databaseBackedSchedule(database).schedule(session("S-1", instructor, studioA, slot(9, 10)));

        SessionSchedule restarted = databaseBackedSchedule(database);

        assertEquals(1, restarted.allSessions().size());
        assertThrows(InvalidBookingException.class,
                () -> restarted.schedule(session("S-2", new Instructor("I-2", "Other"), studioA, slot(9, 10))));
    }

    @Test
    void should_reject_cancelling_a_booking_for_an_unknown_session() {
        assertThrows(InvalidBookingException.class, () -> schedule.cancelBooking("nope", new Member("M-1", "Supun")));
    }

    // S - Simple (recurring weekly classes)
    @Test
    void should_schedule_a_weekly_class_for_the_requested_number_of_weeks() {
        FitnessSession pilates = session("PIL", instructor, studioA, slot(7, 8));

        List<FitnessSession> created = schedule.scheduleWeekly(pilates, 4);

        assertEquals(4, created.size());
        assertEquals(4, schedule.allSessions().size());
        assertEquals(MONDAY.plusWeeks(3), created.get(3).slot().start().toLocalDate());
    }

    @Test
    void should_schedule_no_weekly_sessions_when_one_week_clashes() {
        schedule.schedule(new FitnessSession("BLOCK", "Blocker", new Instructor("I-2", "Other"), studioA,
                new TimeSlot(LocalDateTime.of(2026, 10, 19, 7, 0), LocalDateTime.of(2026, 10, 19, 8, 0)), 10, List.of()));
        FitnessSession pilates = session("PIL", instructor, studioA, slot(7, 8));

        assertThrows(InvalidBookingException.class, () -> schedule.scheduleWeekly(pilates, 4));

        assertEquals(1, schedule.allSessions().size());
    }

    @Test
    void should_reject_a_weekly_class_when_weeks_is_less_than_one() {
        FitnessSession pilates = session("PIL", instructor, studioA, slot(7, 8));

        assertThrows(InvalidBookingException.class, () -> schedule.scheduleWeekly(pilates, 0));
    }
}
