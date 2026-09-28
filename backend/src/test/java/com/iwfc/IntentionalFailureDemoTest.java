package com.iwfc;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * INTENTIONALLY FAILING TESTS (brief: "at least one intentional failing test").
 *
 * Each test states a WRONG expectation on purpose: it expects an illegal action to succeed. The system
 * correctly refuses with the custom exception, so the test FAILS and the failure report names that exception.
 * That failure is the proof that the exception is thrown when the error condition is met.
 *
 * They are tagged and excluded from the normal build so it stays green. Run them on their own with:
 * <pre>mvn test -Pshow-failure</pre>
 * Expected result: 3 failures, one per mandatory custom exception.
 */
@Tag("intentional-failure")
class IntentionalFailureDemoTest {

    private final IwfcFacade system = IwfcBootstrap.seeded();

    @Test
    @DisplayName("FAILS ON PURPOSE: expects a double-booked studio to be accepted -> InvalidBookingException")
    void should_accept_a_double_booked_studio_FAILS_ON_PURPOSE() {
        User instructor = system.findUser("I-2");
        TimeSlot yogaTime = system.findSession("S-1").slot();

        // Studio A is already taken by S-1 at this time; the system throws InvalidBookingException.
        system.scheduleSession(instructor, "CLASH", "Clashing class", new Location("Studio A"), yogaTime, 10, List.of());
    }

    @Test
    @DisplayName("FAILS ON PURPOSE: expects a member to read the maintenance log -> UnauthorizedAccessException")
    void should_let_a_member_read_the_maintenance_log_FAILS_ON_PURPOSE() {
        User member = system.findUser("M-1");

        // Members are not allowed; the system throws UnauthorizedAccessException.
        system.maintenanceActivityLog(member);
    }

    @Test
    @DisplayName("FAILS ON PURPOSE: expects a duplicate equipment id to be accepted -> DuplicateEquipmentException")
    void should_register_equipment_with_an_existing_id_FAILS_ON_PURPOSE() {
        User admin = system.findUser("A-1");

        // TM-01 already exists; the system throws DuplicateEquipmentException.
        system.addEquipment(admin, EquipmentType.TREADMILL, "TM-01", "Copy of treadmill", new Location("Cardio Zone"));
    }
}
