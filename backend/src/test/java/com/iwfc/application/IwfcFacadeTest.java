package com.iwfc.application;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Facade pattern (structural): one simple entry point over inventory, sessions, maintenance and notifications. */
class IwfcFacadeTest {

    private static final LocalDate NEXT_MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    private final IwfcFacade emptySystem = IwfcBootstrap.empty();
    private final IwfcFacade system = IwfcBootstrap.seeded();

    private static TimeSlot slot(int startHour, int endHour) {
        return new TimeSlot(NEXT_MONDAY.atTime(startHour, 0), NEXT_MONDAY.atTime(endHour, 0));
    }

    // Z
    @Test
    void should_start_with_no_equipment_or_sessions_when_the_system_is_empty() {
        assertTrue(emptySystem.listEquipment().isEmpty());
        assertTrue(emptySystem.availableSessions().isEmpty());
    }

    // O
    @Test
    void should_log_in_a_seeded_user_by_id() {
        User admin = system.findUser("A-1");

        assertEquals("Administrator", admin.roleName());
    }

    // M
    @Test
    void should_come_with_sample_equipment_users_and_sessions_when_seeded() {
        assertTrue(system.listEquipment().size() >= 6);
        assertFalse(system.availableSessions().isEmpty());
        assertEquals(5, system.listUsers(system.findUser("A-1")).size());
    }

    // B / I - maintenance workflow through the facade with notifications
    @Test
    void should_notify_the_instructor_at_each_step_when_a_fault_goes_through_the_workflow() {
        User instructor = system.findUser("I-1");
        User admin = system.findUser("A-1");

        MaintenanceRequest request = system.reportFault(instructor, "SB-04", "Resistance failure", Urgency.HIGH);
        system.assignMaintenance(admin, request.id(), "Technician Kamal");
        system.updateMaintenanceProgress(admin, request.id(), "Part ordered");
        system.completeMaintenance(admin, request.id());

        assertEquals(3, system.inbox(instructor).size());
        assertEquals(1, system.maintenanceRequests(admin).size());
        assertEquals(3, system.maintenanceActivityLog(admin).size());
    }

    @Test
    void should_let_a_member_book_a_seeded_session_and_receive_a_notification() {
        User member = system.findUser("M-1");
        FitnessSession session = system.availableSessions().get(0);

        system.bookSession(session.id(), member);

        assertEquals(1, system.inbox(member).size());
        assertEquals(1, system.findSession(session.id()).bookedCount());
    }

    @Test
    void should_raise_an_alert_in_the_activity_log_when_usage_reaches_the_threshold() {
        User instructor = system.findUser("I-1");
        User admin = system.findUser("A-1");

        system.logEquipmentUsage(instructor, "TM-01", 100);

        assertTrue(system.maintenanceActivityLog(admin).stream().anyMatch(line -> line.contains("TM-01")));
    }

    // E - the three mandatory custom exceptions, seen from the outside
    @Test
    void should_throw_unauthorized_when_a_member_opens_the_maintenance_log() {
        User member = system.findUser("M-1");

        assertThrows(UnauthorizedAccessException.class, () -> system.maintenanceRequests(member));
        assertThrows(UnauthorizedAccessException.class, () -> system.maintenanceActivityLog(member));
        assertThrows(UnauthorizedAccessException.class, () -> system.listUsers(member));
    }

    @Test
    void should_throw_duplicate_equipment_when_the_id_is_already_registered() {
        User admin = system.findUser("A-1");

        assertThrows(DuplicateEquipmentException.class,
                () -> system.addEquipment(admin, EquipmentType.TREADMILL, "TM-01", "Another", new Location("Cardio Zone")));
    }

    @Test
    void should_throw_invalid_booking_when_two_sessions_need_the_same_studio_at_the_same_time() {
        User instructor = system.findUser("I-2");
        system.scheduleSession(instructor, "X-1", "Core", new Location("Studio B"), slot(15, 16), 10, List.of());

        assertThrows(InvalidBookingException.class, () -> system.scheduleSession(
                system.findUser("I-1"), "X-2", "Stretch", new Location("Studio B"), slot(15, 16), 10, List.of()));
    }

    @Test
    void should_throw_invalid_booking_when_a_session_is_outside_operating_hours() {
        User instructor = system.findUser("I-1");

        assertThrows(InvalidBookingException.class, () -> system.scheduleSession(
                instructor, "X-3", "Midnight", new Location("Studio B"), slot(1, 2), 10, List.of()));
    }

    @Test
    void should_throw_not_found_when_logging_in_with_an_unknown_id() {
        assertThrows(ResourceNotFoundException.class, () -> system.findUser("nobody"));
    }

    // S
    @Test
    void should_schedule_a_recurring_weekly_class_through_the_facade() {
        User instructor = system.findUser("I-1");

        List<FitnessSession> created = system.scheduleWeeklySession(instructor, "PIL", "Monday Morning Pilates",
                new Location("Studio B"), slot(7, 8), 12, List.of(), 4);

        assertEquals(4, created.size());
    }
}
