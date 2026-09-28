package com.iwfc;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.InvalidStatusTransitionException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Robustness verification (brief: "intentional failing" scenarios). Each test drives the whole system
 * into an error condition on purpose and proves the right custom exception is thrown, carries a useful
 * message, and leaves the system state unchanged.
 */
class RobustnessVerificationTest {

    private static final LocalDate NEXT_MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    private final IwfcFacade system = IwfcBootstrap.seeded();
    private final User admin = system.findUser("A-1");
    private final User instructor = system.findUser("I-1");
    private final User member = system.findUser("M-1");

    @Test
    @DisplayName("Invalid booking: double-booking the same studio is rejected and the schedule is unchanged")
    void should_throw_invalid_booking_and_keep_the_schedule_when_a_studio_is_double_booked() {
        int before = system.allSessions().size();
        TimeSlot yogaTime = system.findSession("S-1").slot();

        InvalidBookingException error = assertThrows(InvalidBookingException.class, () -> system.scheduleSession(
                system.findUser("I-2"), "CLASH", "Clashing class", new Location("Studio A"), yogaTime, 10, List.of()));

        assertTrue(error.getMessage().contains("S-1"), "message should name the session it clashes with");
        assertEquals(before, system.allSessions().size());
    }

    @Test
    @DisplayName("Invalid booking: scheduling outside operating hours is rejected")
    void should_throw_invalid_booking_when_a_session_is_outside_operating_hours() {
        TimeSlot night = new TimeSlot(NEXT_MONDAY.atTime(23, 0), NEXT_MONDAY.atTime(23, 30));

        InvalidBookingException error = assertThrows(InvalidBookingException.class, () -> system.scheduleSession(
                instructor, "LATE", "Late class", new Location("Studio B"), night, 10, List.of()));

        assertTrue(error.getMessage().contains("06:00"));
    }

    @Test
    @DisplayName("Unauthorized access: a member cannot read the administrator's maintenance log")
    void should_throw_unauthorized_access_when_a_member_reads_the_administrator_log() {
        UnauthorizedAccessException error =
                assertThrows(UnauthorizedAccessException.class, () -> system.maintenanceActivityLog(member));

        assertTrue(error.getMessage().contains("Member"));
        assertTrue(error.getMessage().contains("maintenance log"));
    }

    @Test
    @DisplayName("Duplicate data: registering equipment with an existing id is rejected and inventory is unchanged")
    void should_throw_duplicate_equipment_and_keep_the_inventory_when_the_id_exists() {
        int before = system.listEquipment().size();

        DuplicateEquipmentException error = assertThrows(DuplicateEquipmentException.class, () -> system.addEquipment(
                admin, EquipmentType.TREADMILL, "TM-01", "Copy of treadmill", new Location("Cardio Zone")));

        assertTrue(error.getMessage().contains("TM-01"));
        assertEquals(before, system.listEquipment().size());
        assertEquals("Treadmill 01", system.findEquipment("TM-01").name());
    }

    @Test
    @DisplayName("Workflow: skipping a step is rejected and equipment stays faulty")
    void should_throw_invalid_transition_when_a_pending_request_is_completed_directly() {
        MaintenanceRequest request = system.reportFault(instructor, "TM-02", "Belt slipping", Urgency.MEDIUM);

        assertThrows(InvalidStatusTransitionException.class, () -> system.completeMaintenance(admin, request.id()));

        assertEquals("FAULTY", system.findEquipment("TM-02").status().name());
    }

    @Test
    @DisplayName("Faulty equipment cannot be put into a new session")
    void should_throw_invalid_booking_when_a_session_needs_faulty_equipment() {
        system.reportFault(instructor, "SB-03", "Pedal broken", Urgency.HIGH);
        TimeSlot slot = new TimeSlot(NEXT_MONDAY.atTime(14, 0), NEXT_MONDAY.atTime(15, 0));

        assertThrows(InvalidBookingException.class, () -> system.scheduleSession(
                instructor, "SPIN-2", "Spin", new Location("Spin Studio"), slot, 8, List.of("SB-03")));
    }
}
