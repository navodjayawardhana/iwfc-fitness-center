package com.iwfc.application;

import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.usecase.BookSessionUseCase;
import com.iwfc.application.usecase.EquipmentInventoryUseCase;
import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.SessionSchedule;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.persistence.InMemoryRepository;
import com.iwfc.support.CopyingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookSessionUseCaseTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    private final Repository<Equipment, String> equipment = new InMemoryRepository<>(Equipment::id);
    private final NotificationService notifications = new NotificationService();
    private final EquipmentInventoryUseCase inventory =
            new EquipmentInventoryUseCase(equipment, new EquipmentFactory(), notifications);
    private final BookSessionUseCase sessions = new BookSessionUseCase(
            new SessionSchedule(LocalTime.of(6, 0), LocalTime.of(22, 0), new InMemoryRepository<>(FitnessSession::id)), equipment, inventory, notifications);
    private final Administrator admin = new Administrator("A-1", "Prasad");
    private final Instructor instructor = new Instructor("I-1", "Nushfa");
    private final Instructor otherInstructor = new Instructor("I-2", "Kamal");
    private final Member member = new Member("M-1", "Supun");
    private final Location studioA = new Location("Studio A");

    private static TimeSlot slot(int startHour, int endHour) {
        return new TimeSlot(MONDAY.atTime(startHour, 0), MONDAY.atTime(endHour, 0));
    }

    @BeforeEach
    void registerBike() {
        inventory.add(admin, EquipmentType.SPIN_BIKE, "SB-04", "Spin Bike 04", new Location("Cardio Zone"));
    }

    // Z
    @Test
    void should_show_no_available_sessions_when_nothing_is_scheduled() {
        assertTrue(sessions.availableSessions().isEmpty());
    }

    // O
    @Test
    void should_show_a_scheduled_session_as_available() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());

        assertEquals(1, sessions.availableSessions().size());
    }

    // M
    @Test
    void should_book_many_members_into_one_session_and_tell_each_of_them() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());
        Member second = new Member("M-2", "Navod");

        sessions.book("S-1", member);
        sessions.book("S-1", second);

        assertEquals(2, sessions.find("S-1").bookedCount());
        assertEquals(1, notifications.inboxOf("M-1").size());
        assertEquals(1, notifications.inboxOf("M-2").size());
    }

    @Test
    void should_schedule_a_recurring_weekly_class() {
        List<FitnessSession> created = sessions.scheduleWeekly(instructor, "PIL", "Monday Morning Pilates",
                studioA, slot(7, 8), 10, List.of(), 4);

        assertEquals(4, created.size());
        assertEquals(4, sessions.availableSessions().size());
    }

    @Test
    void should_add_a_wellness_tip_to_the_booking_confirmation() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());

        sessions.book("S-1", member);

        String message = notifications.inboxOf("M-1").get(0);
        assertTrue(message.startsWith("Booked: Yoga"));
        assertTrue(message.contains("Tip:"));
    }

    // B
    @Test
    void should_stop_offering_a_session_once_it_is_full() {
        sessions.schedule(instructor, "S-1", "Tiny", studioA, slot(9, 10), 1, List.of());

        sessions.book("S-1", member);

        assertTrue(sessions.availableSessions().isEmpty());
    }

    // I
    @Test
    void should_tell_booked_members_when_the_instructor_cancels_the_session() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());
        sessions.book("S-1", member);

        sessions.cancelSession(instructor, "S-1");

        assertTrue(sessions.availableSessions().isEmpty());
        assertTrue(notifications.inboxOf("M-1").stream().anyMatch(message -> message.contains("cancelled")));
    }

    @Test
    void should_log_equipment_usage_for_the_session_length_when_the_session_is_completed() {
        sessions.schedule(instructor, "S-1", "Spin", studioA, slot(9, 11), 10, List.of("SB-04"));

        sessions.completeSession(instructor, "S-1");

        assertEquals(2.0, inventory.find("SB-04").totalUsageHours());
    }

    // E - the invalid booking exceptions
    @Test
    void should_throw_invalid_booking_when_the_same_studio_is_double_booked() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());

        assertThrows(InvalidBookingException.class,
                () -> sessions.schedule(otherInstructor, "S-2", "HIIT", studioA, slot(9, 10), 10, List.of()));
    }

    @Test
    void should_throw_invalid_booking_when_the_same_equipment_is_double_booked() {
        sessions.schedule(instructor, "S-1", "Spin", studioA, slot(9, 10), 10, List.of("SB-04"));

        assertThrows(InvalidBookingException.class, () -> sessions.schedule(otherInstructor, "S-2", "Spin 2",
                new Location("Studio B"), slot(9, 10), 10, List.of("SB-04")));
    }

    @Test
    void should_throw_invalid_booking_when_a_session_is_outside_operating_hours() {
        assertThrows(InvalidBookingException.class,
                () -> sessions.schedule(instructor, "S-1", "Late class", studioA,
                        new TimeSlot(MONDAY.atTime(21, 30), MONDAY.atTime(22, 30)), 10, List.of()));
    }

    @Test
    void should_throw_invalid_booking_when_a_member_books_the_same_session_twice() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());
        sessions.book("S-1", member);

        assertThrows(InvalidBookingException.class, () -> sessions.book("S-1", member));
    }

    @Test
    void should_throw_unauthorized_when_a_member_tries_to_schedule_a_session() {
        assertThrows(UnauthorizedAccessException.class,
                () -> sessions.schedule(member, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of()));
    }

    @Test
    void should_throw_unauthorized_when_an_administrator_tries_to_book() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());

        assertThrows(UnauthorizedAccessException.class, () -> sessions.book("S-1", admin));
    }

    @Test
    void should_throw_not_found_when_the_session_or_equipment_does_not_exist() {
        assertThrows(ResourceNotFoundException.class, () -> sessions.find("missing"));
        assertThrows(ResourceNotFoundException.class,
                () -> sessions.schedule(instructor, "S-1", "Spin", studioA, slot(9, 10), 10, List.of("NOPE")));
    }

    @Test
    void should_not_change_the_schedule_when_a_booking_is_rejected() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());

        assertThrows(InvalidBookingException.class,
                () -> sessions.schedule(otherInstructor, "S-2", "HIIT", studioA, slot(9, 10), 10, List.of()));

        assertEquals(1, sessions.availableSessions().size());
    }

    @Test
    void should_keep_bookings_when_the_session_store_returns_copies_like_a_database() {
        BookSessionUseCase databaseBacked = new BookSessionUseCase(
                new SessionSchedule(LocalTime.of(6, 0), LocalTime.of(22, 0), new CopyingSessionRepository()),
                equipment, inventory, notifications);
        databaseBacked.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());

        databaseBacked.book("S-1", member);
        assertEquals(1, databaseBacked.find("S-1").bookedCount());

        databaseBacked.cancelBooking("S-1", member);
        assertEquals(0, databaseBacked.find("S-1").bookedCount());
    }

    // S
    @Test
    void should_let_a_member_cancel_their_own_booking() {
        sessions.schedule(instructor, "S-1", "Yoga", studioA, slot(9, 10), 10, List.of());
        sessions.book("S-1", member);

        sessions.cancelBooking("S-1", member);

        assertEquals(0, sessions.find("S-1").bookedCount());
    }
}
